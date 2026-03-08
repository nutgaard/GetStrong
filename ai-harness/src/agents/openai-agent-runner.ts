import { readFile, writeFile } from "node:fs/promises";
import { resolve, sep } from "node:path";

import { Agent, run, tool, type RunContext } from "@openai/agents";
import { z } from "zod";

import type { AgentRunner } from "./contracts";
import { commitAll, listChangedFiles } from "../core/git";
import { RunLogger } from "../io/logger";
import { runCommand } from "../tools/command-runner";
import {
  architectOutputSchema,
  implementerOutputSchema,
  plannerOutputSchema,
  verifierOutputSchema,
  type ArchitectOutput,
  type GovernanceDocs,
  type ImplementerOutput,
  type PlannerOutput,
  type TaskRecord,
  type VerifierOutput,
} from "../types";

interface HarnessToolContext {
  repoRoot: string;
  logger: RunLogger;
}

const commandInputSchema = z.object({
  command: z.string().min(1),
  timeoutMs: z.number().int().min(100).max(60 * 60_000).optional(),
});

const readFileInputSchema = z.object({
  path: z.string().min(1),
  maxChars: z.number().int().min(200).max(80_000).optional(),
});

const writeFileInputSchema = z.object({
  path: z.string().min(1),
  content: z.string(),
});

const commitInputSchema = z.object({
  message: z.string().min(1),
});

function truncate(text: string, limit = 8_000): string {
  if (text.length <= limit) {
    return text;
  }
  return `${text.slice(0, limit)}\n...<truncated ${text.length - limit} chars>`;
}

function ensureRepoPath(repoRoot: string, targetPath: string): string {
  const resolved = resolve(repoRoot, targetPath);
  const normalizedRoot = `${resolve(repoRoot)}${sep}`;
  if (!resolved.startsWith(normalizedRoot) && resolved !== resolve(repoRoot)) {
    throw new Error(`Path escapes repository root: ${targetPath}`);
  }
  return resolved;
}

function plannerInstructions(): string {
  return [
    "You are the planner agent.",
    "Your only product input is PRD.md content.",
    "Generate and maintain TASKS.yml tasks that are small, testable, and dependency-aware.",
    "Preserve progress for tasks already in review/done unless PRD clearly invalidates them.",
    "Each task must have explicit acceptance_criteria and sensible expected_files when known.",
    "Use priorities P0..P3 and statuses backlog|ready|in_progress|review|done|blocked.",
    "Set blocked=true only if planning cannot proceed safely.",
    "Return structured output only.",
  ].join("\n");
}

function architectInstructions(): string {
  return [
    "You are the architect agent.",
    "Provide high-level architecture direction for the selected task.",
    "Focus on principles, module constraints, dependency rules, and major risks.",
    "Do not micromanage exact file edits or command-by-command implementation steps.",
    "Emit required ADR identifiers when architectural/dependency changes are needed.",
    "If previous governance violations are provided, adjust constraints to resolve them strategically.",
    "Return structured output only.",
  ].join("\n");
}

function implementerInstructions(): string {
  return [
    "You are the implementer agent.",
    "Implement exactly the selected task using architect high-level constraints.",
    "You are free to choose low-level coding details.",
    "Do not ignore architect non-negotiable constraints or dependency rules.",
    "Create commits for coherent progress chunks.",
    "If verification or governance violations are provided, resolve them first.",
    "Return structured output only.",
  ].join("\n");
}

function verifierInstructions(): string {
  return [
    "You are the verifier agent.",
    "Evaluate check outputs and implementation context.",
    "If checks fail, provide concrete repairHints.",
    "Return structured output only.",
  ].join("\n");
}

function createImplementerTools() {
  const runRepoCommand = tool({
    name: "run_repo_command",
    description: "Run a shell command from repository root and return exit code/stdout/stderr.",
    parameters: commandInputSchema,
    execute: async (input, context) => {
      const { repoRoot, logger } = getHarnessContext(context);
      const result = await runCommand(input.command, {
        cwd: repoRoot,
        timeoutMs: input.timeoutMs,
      });

      await logger.event("tool.run_repo_command", {
        command: input.command,
        exitCode: result.exitCode,
      });

      return {
        exitCode: result.exitCode,
        stdout: truncate(result.stdout),
        stderr: truncate(result.stderr),
        durationMs: Math.round(result.durationMs),
      };
    },
  });

  const readRepoFile = tool({
    name: "read_repo_file",
    description: "Read a UTF-8 file within repository root.",
    parameters: readFileInputSchema,
    execute: async (input, context) => {
      const { repoRoot } = getHarnessContext(context);
      const path = ensureRepoPath(repoRoot, input.path);
      const raw = await readFile(path, "utf8");
      return truncate(raw, input.maxChars ?? 20_000);
    },
  });

  const writeRepoFile = tool({
    name: "write_repo_file",
    description: "Write a UTF-8 file within repository root.",
    parameters: writeFileInputSchema,
    execute: async (input, context) => {
      const { repoRoot, logger } = getHarnessContext(context);
      const path = ensureRepoPath(repoRoot, input.path);
      await writeFile(path, input.content, "utf8");
      await logger.event("tool.write_repo_file", { path: input.path, bytes: input.content.length });
      return `Wrote ${input.content.length} bytes to ${input.path}`;
    },
  });

  const gitCommit = tool({
    name: "git_commit_all",
    description: "Stage all repository changes and commit with message.",
    parameters: commitInputSchema,
    execute: async (input, context) => {
      const { repoRoot, logger } = getHarnessContext(context);
      const sha = await commitAll({ cwd: repoRoot }, input.message);
      await logger.event("tool.git_commit_all", { message: input.message, sha });
      if (!sha) {
        return "No changes to commit.";
      }
      return `Committed ${sha}`;
    },
  });

  const changedFiles = tool({
    name: "git_changed_files",
    description: "List currently changed files in git working tree.",
    parameters: z.object({}),
    execute: async (_input, context) => {
      const { repoRoot } = getHarnessContext(context);
      return await listChangedFiles({ cwd: repoRoot });
    },
  });

  return [runRepoCommand, readRepoFile, writeRepoFile, gitCommit, changedFiles];
}

function requireStructuredOutput<T>(value: T | undefined, label: string): T {
  if (!value) {
    throw new Error(`Agent returned no structured output for ${label}`);
  }
  return value;
}

export class OpenAIAgentRunner implements AgentRunner {
  private readonly plannerAgent: Agent<any, any>;
  private readonly architectAgent: Agent<any, any>;
  private readonly implementerAgent: Agent<any, any>;
  private readonly verifierAgent: Agent<any, any>;
  private readonly repoRoot: string;
  private readonly logger: RunLogger;

  constructor(args: { repoRoot: string; logger: RunLogger }) {
    this.repoRoot = args.repoRoot;
    this.logger = args.logger;

    this.plannerAgent = new Agent({
      name: "planner",
      instructions: plannerInstructions(),
      outputType: plannerOutputSchema,
      model: "gpt-5.1",
      modelSettings: {
        reasoning: { effort: "medium" },
      },
    });

    this.architectAgent = new Agent({
      name: "architect",
      instructions: architectInstructions(),
      outputType: architectOutputSchema,
      model: "gpt-5.4",
      modelSettings: {
        reasoning: { effort: "high" },
      },
    });

    this.implementerAgent = new Agent<HarnessToolContext, typeof implementerOutputSchema>({
      name: "implementer",
      instructions: implementerInstructions(),
      outputType: implementerOutputSchema,
      model: "gpt-5.3-codex",
      modelSettings: {
        reasoning: { effort: "medium" },
      },
      tools: createImplementerTools(),
    });

    this.verifierAgent = new Agent({
      name: "verifier",
      instructions: verifierInstructions(),
      outputType: verifierOutputSchema,
      model: "gpt-5-mini",
      modelSettings: {
        reasoning: { effort: "low" },
      },
    });
  }

  async runPlanner(input: { governance: GovernanceDocs }): Promise<PlannerOutput> {
    await this.logger.event("agent.planner.start", {});
    const runResult = await run(
      this.plannerAgent,
      [
        "PRD.md:",
        input.governance.prd,
        "",
        "Current TASKS.yml (json):",
        JSON.stringify(input.governance.tasks, null, 2),
      ].join("\n"),
      { maxTurns: 8 },
    );

    const output = requireStructuredOutput(runResult.finalOutput, "planner");
    const parsed = plannerOutputSchema.parse(output);
    await this.logger.event("agent.planner", parsed);
    return parsed;
  }

  async runArchitect(input: {
    task: TaskRecord;
    governance: GovernanceDocs;
    previousViolations?: string[];
  }): Promise<ArchitectOutput> {
    await this.logger.event("agent.architect.start", {
      taskId: input.task.id,
    });
    const lines = [
      "Selected task:",
      JSON.stringify(input.task, null, 2),
      "",
      "PRD.md:",
      input.governance.prd,
      "",
      "ARCHITECTURE.md:",
      input.governance.architecture,
      "",
      "Current ADR files (name -> content):",
      JSON.stringify(input.governance.adrs, null, 2),
    ];

    if (input.previousViolations && input.previousViolations.length > 0) {
      lines.push("", "Previous governance violations:", input.previousViolations.map((v) => `- ${v}`).join("\n"));
    }

    const runResult = await run(this.architectAgent, lines.join("\n"), { maxTurns: 8 });

    const output = requireStructuredOutput(runResult.finalOutput, "architect");
    const parsed = architectOutputSchema.parse(output);
    await this.logger.event("agent.architect", parsed);
    return parsed;
  }

  async runImplementer(input: {
    task: TaskRecord;
    governance: GovernanceDocs;
    architect: ArchitectOutput;
    repairAttempt: number;
    verificationFailure?: string;
    verificationLogSnippet?: string;
    governanceViolations?: string[];
  }): Promise<ImplementerOutput> {
    await this.logger.event("agent.implementer.start", {
      taskId: input.task.id,
      repairAttempt: input.repairAttempt,
    });
    const lines = [
      "Selected task:",
      JSON.stringify(input.task, null, 2),
      "",
      "PRD.md:",
      input.governance.prd,
      "",
      "Current TASKS.yml (json):",
      JSON.stringify(input.governance.tasks, null, 2),
      "",
      "Architect strategic guidance:",
      JSON.stringify(input.architect, null, 2),
      "",
      "Current architecture document:",
      input.governance.architecture,
      "",
      "Current ADR files (name -> content):",
      JSON.stringify(input.governance.adrs, null, 2),
      "",
      `Repair attempt: ${input.repairAttempt}`,
    ];

    if (input.verificationFailure) {
      lines.push("", `Verification failure: ${input.verificationFailure}`);
    }
    if (input.verificationLogSnippet) {
      lines.push("", "Verification log snippet:", input.verificationLogSnippet);
    }
    if (input.governanceViolations && input.governanceViolations.length > 0) {
      lines.push("", "Governance violations:", input.governanceViolations.map((v) => `- ${v}`).join("\n"));
    }

    const runResult = await run(this.implementerAgent, lines.join("\n"), {
      maxTurns: 20,
      context: {
        repoRoot: this.repoRoot,
        logger: this.logger,
      },
    });

    const output = requireStructuredOutput(runResult.finalOutput, "implementer");
    const parsed = implementerOutputSchema.parse(output);
    await this.logger.event("agent.implementer", parsed);
    return parsed;
  }

  async runVerifier(input: {
    task: TaskRecord;
    checks: string[];
    passed: boolean;
    failedStep: string | null;
    verificationLogSnippet: string;
    implementer: ImplementerOutput;
  }): Promise<VerifierOutput> {
    await this.logger.event("agent.verifier.start", {
      taskId: input.task.id,
    });
    const runResult = await run(
      this.verifierAgent,
      [
        "Selected task:",
        JSON.stringify(input.task, null, 2),
        "",
        "Verification status:",
        JSON.stringify(
          {
            checks: input.checks,
            passed: input.passed,
            failedStep: input.failedStep,
          },
          null,
          2,
        ),
        "",
        "Implementer output:",
        JSON.stringify(input.implementer, null, 2),
        "",
        "Verification log snippet:",
        input.verificationLogSnippet,
      ].join("\n"),
      { maxTurns: 5 },
    );

    const output = requireStructuredOutput(runResult.finalOutput, "verifier");
    const parsed = verifierOutputSchema.parse(output);
    await this.logger.event("agent.verifier", parsed);
    return parsed;
  }
}

function getHarnessContext(context?: RunContext<unknown>): HarnessToolContext {
  const resolved = context?.context as HarnessToolContext | undefined;
  if (!resolved || !resolved.repoRoot) {
    throw new Error("Missing repoRoot context");
  }
  return resolved;
}
