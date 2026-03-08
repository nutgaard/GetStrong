import { readFile, writeFile } from "node:fs/promises";
import { resolve, sep } from "node:path";

import { Agent, run, tool, type RunContext, type RunStreamEvent } from "@openai/agents";
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

export type AgentRole = "planner" | "architect" | "implementer" | "verifier";

export interface NormalizedAgentStreamEvent {
  kind: string;
  payload: Record<string, unknown>;
}

interface StreamEventCounters {
  modelResponsesStarted: number;
  modelResponsesDone: number;
  reasoningItems: number;
  toolCalls: number;
  toolOutputs: number;
  handoffRequested: number;
  handoffOccurred: number;
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

function createEmptyCounters(): StreamEventCounters {
  return {
    modelResponsesStarted: 0,
    modelResponsesDone: 0,
    reasoningItems: 0,
    toolCalls: 0,
    toolOutputs: 0,
    handoffRequested: 0,
    handoffOccurred: 0,
  };
}

function maybeString(value: unknown): string | undefined {
  return typeof value === "string" && value.trim().length > 0 ? value.trim() : undefined;
}

function describeToolItem(item: unknown): Record<string, unknown> {
  const rawItem = (item as { rawItem?: unknown } | undefined)?.rawItem ?? item;
  const raw = (rawItem && typeof rawItem === "object" ? rawItem : {}) as Record<string, unknown>;

  const details: Record<string, unknown> = {};
  const toolType = maybeString(raw.type);
  const name = maybeString(raw.name);
  const callId = maybeString(raw.callId);

  if (toolType) {
    details.toolType = toolType;
  }
  if (name) {
    details.name = name;
  }
  if (callId) {
    details.callId = callId;
  }

  return details;
}

export function extractReasoningSnippet(item: unknown, maxChars = 160): string | undefined {
  const rawItem = (item as { rawItem?: unknown } | undefined)?.rawItem ?? item;
  if (!rawItem || typeof rawItem !== "object") {
    return undefined;
  }

  const raw = rawItem as Record<string, unknown>;

  const fromRawContent = Array.isArray(raw.rawContent)
    ? raw.rawContent
        .map((entry) => (entry && typeof entry === "object" ? maybeString((entry as Record<string, unknown>).text) : undefined))
        .filter((entry): entry is string => Boolean(entry))
        .join(" ")
    : "";

  const fromContent = Array.isArray(raw.content)
    ? raw.content
        .map((entry) => (entry && typeof entry === "object" ? maybeString((entry as Record<string, unknown>).text) : undefined))
        .filter((entry): entry is string => Boolean(entry))
        .join(" ")
    : "";

  const text = fromRawContent || fromContent;
  if (!text) {
    return undefined;
  }

  return text.length > maxChars ? `${text.slice(0, maxChars)}...` : text;
}

export function normalizeAgentStreamEvent(role: AgentRole, event: RunStreamEvent): NormalizedAgentStreamEvent[] {
  if (event.type === "raw_model_stream_event") {
    const raw = event.data as { type?: unknown; response?: { usage?: Record<string, unknown> } };
    const dataType = maybeString(raw.type);

    if (dataType === "response_started") {
      return [
        {
          kind: `agent.${role}.model.response_started`,
          payload: {},
        },
      ];
    }

    if (dataType === "response_done") {
      const usage = raw.response?.usage;
      return [
        {
          kind: `agent.${role}.model.response_done`,
          payload: {
            totalTokens: typeof usage?.totalTokens === "number" ? usage.totalTokens : undefined,
            inputTokens: typeof usage?.inputTokens === "number" ? usage.inputTokens : undefined,
            outputTokens: typeof usage?.outputTokens === "number" ? usage.outputTokens : undefined,
          },
        },
      ];
    }

    // Suppress token-by-token deltas in default console output.
    if (dataType === "output_text_delta") {
      return [];
    }

    return [];
  }

  if (event.type === "run_item_stream_event") {
    if (event.name === "reasoning_item_created") {
      const snippet = extractReasoningSnippet(event.item);
      return [
        {
          kind: `agent.${role}.reasoning_item_created`,
          payload: snippet ? { snippet } : {},
        },
      ];
    }

    if (event.name === "tool_called") {
      return [
        {
          kind: `agent.${role}.tool_called`,
          payload: describeToolItem(event.item),
        },
      ];
    }

    if (event.name === "tool_output") {
      return [
        {
          kind: `agent.${role}.tool_output`,
          payload: describeToolItem(event.item),
        },
      ];
    }

    if (event.name === "handoff_requested") {
      return [
        {
          kind: `agent.${role}.handoff_requested`,
          payload: {},
        },
      ];
    }

    if (event.name === "handoff_occurred") {
      return [
        {
          kind: `agent.${role}.handoff_occurred`,
          payload: {},
        },
      ];
    }
  }

  return [];
}

function applyCounter(counters: StreamEventCounters, kind: string): void {
  if (kind.endsWith(".model.response_started")) {
    counters.modelResponsesStarted += 1;
  } else if (kind.endsWith(".model.response_done")) {
    counters.modelResponsesDone += 1;
  } else if (kind.endsWith(".reasoning_item_created")) {
    counters.reasoningItems += 1;
  } else if (kind.endsWith(".tool_called")) {
    counters.toolCalls += 1;
  } else if (kind.endsWith(".tool_output")) {
    counters.toolOutputs += 1;
  } else if (kind.endsWith(".handoff_requested")) {
    counters.handoffRequested += 1;
  } else if (kind.endsWith(".handoff_occurred")) {
    counters.handoffOccurred += 1;
  }
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

  private async runAgentWithStreaming<TOutput>(args: {
    role: AgentRole;
    agent: Agent<any, any>;
    input: string;
    options?: {
      maxTurns?: number;
      context?: HarnessToolContext;
    };
    parseOutput: (output: unknown) => TOutput;
    donePayload: (parsed: TOutput) => Record<string, unknown>;
  }): Promise<TOutput> {
    const startedAt = Date.now();
    const counters = createEmptyCounters();

    await this.logger.event(`agent.${args.role}.start`, {});

    try {
      const streamed = await run(args.agent, args.input, {
        ...args.options,
        stream: true,
      });

      for await (const event of streamed) {
        const normalized = normalizeAgentStreamEvent(args.role, event);
        for (const entry of normalized) {
          applyCounter(counters, entry.kind);
          await this.logger.event(entry.kind, entry.payload);
        }
      }

      const output = requireStructuredOutput(streamed.finalOutput, args.role);
      const parsed = args.parseOutput(output);

      await this.logger.event(`agent.${args.role}.done`, {
        elapsedMs: Date.now() - startedAt,
        ...counters,
        ...args.donePayload(parsed),
      });

      return parsed;
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      await this.logger.event(`agent.${args.role}.error`, {
        message,
      });
      throw error;
    }
  }

  async runPlanner(input: { governance: GovernanceDocs }): Promise<PlannerOutput> {
    return await this.runAgentWithStreaming<PlannerOutput>({
      role: "planner",
      agent: this.plannerAgent,
      input: [
        "PRD.md:",
        input.governance.prd,
        "",
        "Current TASKS.yml (json):",
        JSON.stringify(input.governance.tasks, null, 2),
      ].join("\n"),
      options: {
        maxTurns: 8,
      },
      parseOutput: (output: unknown) => plannerOutputSchema.parse(output),
      donePayload: (result) => ({
        blocked: result.blocked,
        taskCount: result.tasks.length,
      }),
    });
  }

  async runArchitect(input: {
    task: TaskRecord;
    governance: GovernanceDocs;
    previousViolations?: string[];
  }): Promise<ArchitectOutput> {
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

    return await this.runAgentWithStreaming<ArchitectOutput>({
      role: "architect",
      agent: this.architectAgent,
      input: lines.join("\n"),
      options: {
        maxTurns: 8,
      },
      parseOutput: (output: unknown) => architectOutputSchema.parse(output),
      donePayload: (result) => ({
        taskId: input.task.id,
        principlesCount: result.principles.length,
        requiredAdrsCount: result.requiredAdrs.length,
      }),
    });
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

    return await this.runAgentWithStreaming<ImplementerOutput>({
      role: "implementer",
      agent: this.implementerAgent,
      input: lines.join("\n"),
      options: {
        maxTurns: 20,
        context: {
          repoRoot: this.repoRoot,
          logger: this.logger,
        },
      },
      parseOutput: (output: unknown) => implementerOutputSchema.parse(output),
      donePayload: (result) => ({
        taskId: input.task.id,
        repairAttempt: input.repairAttempt,
        commitsCount: result.commits.length,
        touchedFilesCount: result.touchedFiles.length,
      }),
    });
  }

  async runVerifier(input: {
    task: TaskRecord;
    checks: string[];
    passed: boolean;
    failedStep: string | null;
    verificationLogSnippet: string;
    implementer: ImplementerOutput;
  }): Promise<VerifierOutput> {
    return await this.runAgentWithStreaming<VerifierOutput>({
      role: "verifier",
      agent: this.verifierAgent,
      input: [
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
      options: {
        maxTurns: 5,
      },
      parseOutput: (output: unknown) => verifierOutputSchema.parse(output),
      donePayload: (result) => ({
        taskId: input.task.id,
        passed: result.passed,
        failuresCount: result.failures.length,
      }),
    });
  }
}

function getHarnessContext(context?: RunContext<unknown>): HarnessToolContext {
  const resolved = context?.context as HarnessToolContext | undefined;
  if (!resolved || !resolved.repoRoot) {
    throw new Error("Missing repoRoot context");
  }
  return resolved;
}
