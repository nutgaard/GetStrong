import type { AgentRunner } from "../agents/contracts";
import { OpenAIAgentRunner } from "../agents/openai-agent-runner";
import { readFileSafe, tailSnippet } from "../io/files";
import { RunLogger, createRunId } from "../io/logger";
import { runCommand } from "../tools/command-runner";
import {
  commitAll,
  createBranchName,
  createTaskBranch,
  ensureBaseBranch,
  listCommitsSince,
  listFilesSince,
  openPr,
  pushBranch,
  workingTreeClean,
} from "./git";
import { runPreflight, type PreflightOptions } from "./preflight";
import { createPrBody, createPrTitle } from "./pr";
import { runVerification } from "./verification";
import { auditStrategicAdherence } from "../governance/audit";
import { ensureGovernanceScaffold, loadGovernanceDocs, saveArchitecture, saveTasks, upsertAdrs } from "../governance/files";
import { ensureSingleInProgress, selectNextTask, updateTaskStatus } from "../governance/tasks";
import type {
  ExecutionState,
  GovernanceDocs,
  HarnessRequest,
  OrchestratorResult,
  RunArtifactIndex,
  TaskRecord,
  TaskTransition,
  TasksFile,
  VerificationReport,
} from "../types";
import { HarnessError } from "../types";

function nowIso(): string {
  return new Date().toISOString();
}

function createSummary(args: {
  mode: HarnessRequest["mode"];
  selectedTaskId?: string;
  branchName?: string;
  prUrl?: string;
  checksPassed?: boolean;
  governancePassed?: boolean;
  message?: string;
}): string {
  const lines = [`Mode: ${args.mode}`];
  if (args.selectedTaskId) {
    lines.push(`Task: ${args.selectedTaskId}`);
  }
  if (args.branchName) {
    lines.push(`Branch: ${args.branchName}`);
  }
  if (args.prUrl) {
    lines.push(`PR: ${args.prUrl}`);
  }
  if (args.checksPassed !== undefined) {
    lines.push(`Checks passed: ${args.checksPassed ? "yes" : "no"}`);
  }
  if (args.governancePassed !== undefined) {
    lines.push(`Governance audit passed: ${args.governancePassed ? "yes" : "no"}`);
  }
  if (args.message) {
    lines.push(args.message);
  }
  return lines.join("\n");
}

function buildArtifactIndex(state: ExecutionState): RunArtifactIndex {
  return {
    runId: state.runId,
    startedAt: state.branchCreatedAt ?? nowIso(),
    finishedAt: nowIso(),
    outcome: state.verification?.passed
      ? state.governanceAudit?.passed === false
        ? "blocked"
        : "success"
      : state.selectedTaskId
        ? "pr_with_failures"
        : "noop",
    branchName: state.branchName,
    selectedTaskId: state.selectedTaskId,
    commitShas: state.commitShas,
    prUrl: state.prUrl,
    artifactPaths: {},
  };
}

export interface OrchestratorDependencies {
  createAgentRunner?: (args: { repoRoot: string; logger: RunLogger }) => AgentRunner;
  runPreflight?: typeof runPreflight;
  runVerification?: typeof runVerification;
}

export class TaskOrchestrator {
  private readonly repoRoot: string;
  private readonly deps: OrchestratorDependencies;

  constructor(repoRoot: string, deps?: OrchestratorDependencies) {
    this.repoRoot = repoRoot;
    this.deps = deps ?? {};
  }

  async execute(request: HarnessRequest): Promise<OrchestratorResult> {
    const runId = createRunId();
    const logger = new RunLogger(runId, this.repoRoot);
    let loggerReady = false;

    const createAgentRunner =
      this.deps.createAgentRunner ?? ((args: { repoRoot: string; logger: RunLogger }) => new OpenAIAgentRunner(args));
    const preflightRunner = this.deps.runPreflight ?? runPreflight;
    const verificationRunner = this.deps.runVerification ?? runVerification;

    const state: ExecutionState = {
      request,
      runId,
      commitShas: [],
      outputs: {},
      taskTransitions: [],
      docDiffSummary: [],
      repairLoopCount: 0,
    };

    try {
      const preflightOptions = this.resolvePreflightOptions(request);
      const preflight = await preflightRunner(this.repoRoot, preflightOptions);

      if (request.mode === "execute-next" || request.mode === "autopilot") {
        const clean = await workingTreeClean({ cwd: this.repoRoot });
        if (!clean) {
          throw new HarnessError(
            "Working tree is not clean. Commit or stash existing changes before running task execution.",
            4,
          );
        }
      }

      await ensureGovernanceScaffold(this.repoRoot);

      await logger.init();
      loggerReady = true;
      await logger.writeJson("request.json", request);
      await logger.writeJson("preflight.json", preflight);

      const agentRunner = createAgentRunner({ repoRoot: this.repoRoot, logger });

      let result: OrchestratorResult;
      switch (request.mode) {
        case "sync-plan":
          result = await this.runSyncPlan(agentRunner, state, logger);
          break;
        case "execute-next":
          result = await this.runExecuteNext(agentRunner, state, logger, verificationRunner);
          break;
        case "autopilot":
          result = await this.runAutopilot(agentRunner, state, logger, verificationRunner);
          break;
        case "reconcile":
          result = await this.runReconcile(state, logger);
          break;
      }

      await persistStateArtifacts(logger, state, "success", result.summary);
      return result;
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      if (loggerReady) {
        await logger.event("orchestrator.error", { message });
        await persistStateArtifacts(logger, state, "failed", message);
      }

      if (error instanceof HarnessError) {
        return {
          exitCode: error.code,
          runId,
          branchName: state.branchName,
          prUrl: state.prUrl,
          summary: message,
        };
      }

      return {
        exitCode: 4,
        runId,
        branchName: state.branchName,
        prUrl: state.prUrl,
        summary: message,
      };
    }
  }

  private resolvePreflightOptions(request: HarnessRequest): PreflightOptions {
    if (request.mode === "sync-plan") {
      return {
        requireOrigin: false,
        requireGhAuth: false,
        requireOpenAI: true,
      };
    }

    if (request.mode === "reconcile") {
      return {
        requireOrigin: false,
        requireGhAuth: true,
        requireOpenAI: false,
      };
    }

    return {
      requireOrigin: true,
      requireGhAuth: !request.noPr,
      requireOpenAI: true,
    };
  }

  private async runSyncPlan(
    agentRunner: AgentRunner,
    state: ExecutionState,
    logger: RunLogger,
  ): Promise<OrchestratorResult> {
    const governance = await loadGovernanceDocs(this.repoRoot);
    const planner = await agentRunner.runPlanner({ governance });
    state.outputs.planner = planner;
    await logger.writeJson("planner-output.json", planner);

    if (planner.blocked) {
      throw new HarnessError(`Planner blocked: ${(planner.questions ?? []).join(" | ") || "No unblock questions provided."}`, 3);
    }

    const nextTasks: TasksFile = {
      version: 1,
      tasks: planner.tasks,
    };

    await saveTasks(this.repoRoot, nextTasks);
    state.docDiffSummary.push("TASKS.yml updated by planner.");

    return {
      exitCode: 0,
      runId: state.runId,
      summary: createSummary({
        mode: "sync-plan",
        message: `Planner synchronized ${planner.tasks.length} tasks from PRD.md.`,
      }),
    };
  }

  private async runExecuteNext(
    agentRunner: AgentRunner,
    state: ExecutionState,
    logger: RunLogger,
    verificationRunner: typeof runVerification,
  ): Promise<OrchestratorResult> {
    let governance = await loadGovernanceDocs(this.repoRoot);

    const planner = await agentRunner.runPlanner({ governance });
    state.outputs.planner = planner;
    await logger.writeJson("planner-output.json", planner);

    if (planner.blocked) {
      throw new HarnessError(`Planner blocked: ${(planner.questions ?? []).join(" | ") || "No unblock questions provided."}`, 3);
    }

    governance.tasks = {
      version: 1,
      tasks: planner.tasks,
    };
    await saveTasks(this.repoRoot, governance.tasks);
    state.docDiffSummary.push("TASKS.yml synchronized before execution.");

    const selectedTask = selectNextTask(governance.tasks, state.request.taskId);
    if (!selectedTask) {
      return {
        exitCode: 0,
        runId: state.runId,
        summary: createSummary({
          mode: "execute-next",
          message: "No runnable task found (ready + dependencies satisfied).",
        }),
      };
    }

    state.selectedTaskId = selectedTask.id;
    ensureSingleInProgress(governance.tasks, selectedTask.id);

    await ensureBaseBranch({ cwd: this.repoRoot }, state.request.baseBranch);
    const branchName = createBranchName(selectedTask.id, selectedTask.title);
    await createTaskBranch({ cwd: this.repoRoot }, state.request.baseBranch, branchName);
    state.branchName = branchName;
    state.branchCreatedAt = nowIso();

    governance.tasks = updateTaskStatus(governance.tasks, selectedTask.id, "in_progress");
    await saveTasks(this.repoRoot, governance.tasks);
    await this.commitIfChanged(`chore(task): mark ${selectedTask.id} in_progress`);
    this.recordTransition(state, selectedTask.id, "ready", "in_progress", "Execution started");

    let architect = await agentRunner.runArchitect({
      task: selectedTask,
      governance,
    });
    state.outputs.architect = architect;
    await logger.writeJson("architect-output.json", architect);

    await this.applyArchitectDocs(selectedTask, architect, governance, state);
    governance = await loadGovernanceDocs(this.repoRoot);

    let implementer = await agentRunner.runImplementer({
      task: selectedTask,
      governance,
      architect,
      repairAttempt: 0,
    });
    state.outputs.implementer = implementer;

    let lastVerification: VerificationReport | undefined;
    let verifierPassed = false;
    let governanceAuditPassed = false;
    let governanceViolations: string[] = [];

    for (let attempt = 0; attempt <= state.request.repairAttempts; attempt += 1) {
      state.repairLoopCount = attempt;

      await syncImplementationMetadata(state, this.repoRoot, state.request.baseBranch);
      await logger.writeJson(`implementer-output-attempt-${attempt}.json`, state.outputs.implementer);

      const verification = await verificationRunner(this.repoRoot, "full", (fileName: string, body: string) =>
        logger.writeText(fileName, body),
      );
      lastVerification = verification;
      state.verification = verification;

      const verificationLog = await readFileSafe(verification.logsPath);
      const verificationSnippet = tailSnippet(verificationLog);

      const verifier = await agentRunner.runVerifier({
        task: selectedTask,
        checks: verification.checks,
        passed: verification.passed,
        failedStep: verification.failedStep,
        verificationLogSnippet: verificationSnippet,
        implementer,
      });
      state.outputs.verifier = verifier;

      const changedFiles = await listFilesSince({ cwd: this.repoRoot }, state.request.baseBranch);
      const governanceAudit = auditStrategicAdherence({
        task: selectedTask,
        architect,
        changedFiles,
      });
      state.governanceAudit = governanceAudit;
      governanceAuditPassed = governanceAudit.passed;
      governanceViolations = governanceAudit.strategicViolations;

      await logger.writeJson(`verifier-output-attempt-${attempt}.json`, verifier);
      await logger.writeJson(`governance-audit-attempt-${attempt}.json`, governanceAudit);

      verifierPassed = verification.passed;
      if (verification.passed && governanceAudit.passed) {
        break;
      }

      if (attempt >= state.request.repairAttempts) {
        break;
      }

      if (!governanceAudit.passed) {
        architect = await agentRunner.runArchitect({
          task: selectedTask,
          governance,
          previousViolations: governanceViolations,
        });
        state.outputs.architect = architect;
        await this.applyArchitectDocs(selectedTask, architect, governance, state);
        governance = await loadGovernanceDocs(this.repoRoot);
      }

      implementer = await agentRunner.runImplementer({
        task: selectedTask,
        governance,
        architect,
        repairAttempt: attempt + 1,
        verificationFailure: verification.failedStep ?? undefined,
        verificationLogSnippet: verificationSnippet,
        governanceViolations,
      });
      state.outputs.implementer = implementer;
    }

    if (!governanceAuditPassed) {
      governance.tasks = updateTaskStatus(governance.tasks, selectedTask.id, "blocked", {
        blockedReason: `Strategic governance violations unresolved: ${governanceViolations.join(" | ")}`,
      });
      await saveTasks(this.repoRoot, governance.tasks);
      await this.commitIfChanged(`chore(task): mark ${selectedTask.id} blocked`);
      this.recordTransition(state, selectedTask.id, "in_progress", "blocked", "Unresolved strategic governance violations");

      throw new HarnessError(`Task ${selectedTask.id} blocked due to strategic governance violations.`, 3);
    }

    const checksPassed = Boolean(verifierPassed);
    const taskTitle = selectedTask.title;
    let prUrl: string | undefined;

    if (!state.request.noPr) {
      const prTitle = createPrTitle(selectedTask.id, taskTitle);
      const prBody = createPrBody(state);
      const prBodyPath = await logger.writeText("pr-body.md", prBody);

      await pushBranch({ cwd: this.repoRoot }, branchName);
      prUrl = await openPr(
        { cwd: this.repoRoot },
        {
          baseBranch: state.request.baseBranch,
          headBranch: branchName,
          title: prTitle,
          bodyPath: prBodyPath,
          draft: state.request.draftPr,
        },
      );
      state.prUrl = prUrl;
    }

    governance.tasks = updateTaskStatus(governance.tasks, selectedTask.id, "review", {
      prUrl,
    });
    await saveTasks(this.repoRoot, governance.tasks);
    await this.commitIfChanged(`chore(task): mark ${selectedTask.id} review`);
    this.recordTransition(state, selectedTask.id, "in_progress", "review", "Execution completed and sent for review");

    const summary = createSummary({
      mode: "execute-next",
      selectedTaskId: selectedTask.id,
      branchName,
      prUrl,
      checksPassed,
      governancePassed: governanceAuditPassed,
    });

    return {
      exitCode: checksPassed ? 0 : 2,
      runId: state.runId,
      branchName,
      prUrl,
      summary,
    };
  }

  private async runAutopilot(
    agentRunner: AgentRunner,
    state: ExecutionState,
    logger: RunLogger,
    verificationRunner: typeof runVerification,
  ): Promise<OrchestratorResult> {
    const executed: string[] = [];

    for (let iteration = 0; iteration < state.request.maxIterations; iteration += 1) {
      const executeResult = await this.runExecuteNext(agentRunner, state, logger, verificationRunner);
      if (!state.selectedTaskId) {
        return {
          exitCode: 0,
          runId: state.runId,
          summary: createSummary({
            mode: "autopilot",
            message: `Autopilot complete. Executed ${executed.length} tasks.`,
          }),
        };
      }

      executed.push(state.selectedTaskId);
      if (executeResult.exitCode !== 0) {
        return {
          ...executeResult,
          summary: `${executeResult.summary}\nAutopilot stopped after ${executed.length} task(s).`,
        };
      }

      state.selectedTaskId = undefined;
    }

    return {
      exitCode: 3,
      runId: state.runId,
      summary: createSummary({
        mode: "autopilot",
        message: `Autopilot stopped after reaching safety limit of ${state.request.maxIterations} iterations.`,
      }),
    };
  }

  private async runReconcile(state: ExecutionState, _logger: RunLogger): Promise<OrchestratorResult> {
    const governance = await loadGovernanceDocs(this.repoRoot);
    let updated = governance.tasks;
    let doneCount = 0;

    for (const task of governance.tasks.tasks) {
      if (task.status !== "review" || !task.pr_url) {
        continue;
      }

      const view = await runCommand(`gh pr view ${shellEscape(task.pr_url)} --json mergedAt,state`, {
        cwd: this.repoRoot,
      });

      if (view.exitCode !== 0) {
        continue;
      }

      const parsed = JSON.parse(view.stdout) as { mergedAt?: string | null; state?: string };
      if (parsed.mergedAt) {
        updated = updateTaskStatus(updated, task.id, "done");
        this.recordTransition(state, task.id, "review", "done", "PR merged");
        doneCount += 1;
      }
    }

    if (doneCount > 0) {
      await saveTasks(this.repoRoot, updated);
    }

    return {
      exitCode: 0,
      runId: state.runId,
      summary: createSummary({
        mode: "reconcile",
        message: `Marked ${doneCount} task(s) as done.`,
      }),
    };
  }

  private async applyArchitectDocs(
    task: TaskRecord,
    architect: NonNullable<ExecutionState["outputs"]["architect"]>,
    governance: GovernanceDocs,
    state: ExecutionState,
  ): Promise<void> {
    const updatedArchitecture = mergeArchitecture(governance.architecture, task, architect);
    await saveArchitecture(this.repoRoot, updatedArchitecture);
    const adrPaths = await upsertAdrs(this.repoRoot, architect.adrs);

    state.docDiffSummary.push(`ARCHITECTURE.md updated for ${task.id}.`);
    if (adrPaths.length > 0) {
      state.docDiffSummary.push(`ADRs updated: ${adrPaths.map((p) => p.split("/").pop()).join(", ")}`);
    }

    await this.commitIfChanged(`docs(architecture): update guidance for ${task.id}`);
  }

  private async commitIfChanged(message: string): Promise<void> {
    const sha = await commitAll({ cwd: this.repoRoot }, message);
    if (!sha) {
      return;
    }
  }

  private recordTransition(
    state: ExecutionState,
    taskId: string,
    from: TaskTransition["from"],
    to: TaskTransition["to"],
    reason: string,
  ): void {
    state.taskTransitions.push({
      taskId,
      from,
      to,
      reason,
      timestamp: nowIso(),
    });
  }
}

function mergeArchitecture(existing: string, task: TaskRecord, architect: NonNullable<ExecutionState["outputs"]["architect"]>): string {
  const section = [
    `## Task ${task.id}: ${task.title}`,
    "",
    "### Principles",
    ...architect.principles.map((line) => `- ${line}`),
    "",
    "### Module Constraints",
    ...(architect.moduleConstraints.length > 0
      ? architect.moduleConstraints.map((line) => `- ${line}`)
      : ["- None"]),
    "",
    "### Dependency Rules",
    ...(architect.dependencyRules.length > 0 ? architect.dependencyRules.map((line) => `- ${line}`) : ["- None"]),
    "",
    "### Required ADRs",
    ...(architect.requiredAdrs.length > 0 ? architect.requiredAdrs.map((line) => `- ${line}`) : ["- None"]),
    "",
    "### Risks",
    ...(architect.risks.length > 0 ? architect.risks.map((line) => `- ${line}`) : ["- None"]),
    "",
    "### Notes",
    architect.architectureNotes.trim().length > 0 ? architect.architectureNotes.trim() : "No additional notes.",
    "",
  ].join("\n");

  const cleaned = existing.trimEnd();
  return `${cleaned}\n\n${section}`;
}

async function syncImplementationMetadata(
  state: ExecutionState,
  repoRoot: string,
  baseBranch: string,
): Promise<void> {
  const [commits, files] = await Promise.all([
    listCommitsSince({ cwd: repoRoot }, baseBranch),
    listFilesSince({ cwd: repoRoot }, baseBranch),
  ]);

  state.commitShas = commits.map((entry: { sha: string }) => entry.sha);

  const existing = state.outputs.implementer;
  if (!existing) {
    return;
  }

  state.outputs.implementer = {
    ...existing,
    commits: commits.map((entry: { message: string }) => entry.message),
    touchedFiles: files,
  };
}

async function persistStateArtifacts(
  logger: RunLogger,
  state: ExecutionState,
  outcome: RunArtifactIndex["outcome"] | "failed",
  summary: string,
): Promise<void> {
  const index = buildArtifactIndex(state);
  if (outcome && outcome !== "failed") {
    index.outcome = outcome;
  }
  if (outcome === "failed") {
    index.outcome = "failed";
  }

  await logger.writeJson("state.json", state);
  await logger.writeText("summary.txt", `${summary}\n`);
  await logger.writeJson("artifact-index.json", index);
  await logger.writeJson("task-transition-log.json", state.taskTransitions);
  await logger.writeJson("doc-diff-summary.json", state.docDiffSummary);
}

function shellEscape(value: string): string {
  return `'${value.replace(/'/g, `'"'"'`)}'`;
}
