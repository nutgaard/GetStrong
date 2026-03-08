import { afterEach, beforeEach, describe, expect, it, setDefaultTimeout } from "bun:test";
import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

import YAML from "yaml";

import type { AgentRunner } from "../../src/agents/contracts";
import { TaskOrchestrator } from "../../src/core/orchestrator";
import { runChecked } from "../../src/tools/command-runner";
import type {
  ArchitectOutput,
  GovernanceDocs,
  ImplementerOutput,
  PlannerOutput,
  TaskRecord,
  VerificationReport,
  VerifierOutput,
} from "../../src/types";

setDefaultTimeout(20_000);

class FakeAgentRunner implements AgentRunner {
  public readonly calls: string[] = [];
  public readonly implementerInputs: Parameters<AgentRunner["runImplementer"]>[0][] = [];

  private implementerIndex = 0;
  private verifierIndex = 0;
  private architectIndex = 0;

  constructor(
    private readonly args: {
      planner: PlannerOutput;
      architect: ArchitectOutput[];
      implementer: ImplementerOutput[];
      verifier: VerifierOutput[];
    },
  ) {}

  async runPlanner(_input: { governance: GovernanceDocs }): Promise<PlannerOutput> {
    this.calls.push("planner");
    return this.args.planner;
  }

  async runArchitect(_input: Parameters<AgentRunner["runArchitect"]>[0]): Promise<ArchitectOutput> {
    this.calls.push("architect");
    const value = this.args.architect[Math.min(this.architectIndex, this.args.architect.length - 1)];
    this.architectIndex += 1;
    return value;
  }

  async runImplementer(input: Parameters<AgentRunner["runImplementer"]>[0]): Promise<ImplementerOutput> {
    this.calls.push("implementer");
    this.implementerInputs.push(input);
    const value = this.args.implementer[Math.min(this.implementerIndex, this.args.implementer.length - 1)];
    this.implementerIndex += 1;
    return value;
  }

  async runVerifier(_input: Parameters<AgentRunner["runVerifier"]>[0]): Promise<VerifierOutput> {
    this.calls.push("verifier");
    const value = this.args.verifier[Math.min(this.verifierIndex, this.args.verifier.length - 1)];
    this.verifierIndex += 1;
    return value;
  }
}

async function createTempRepo(): Promise<string> {
  const repoRoot = await mkdtemp(join(tmpdir(), "getstrong-ai-harness-"));
  await runChecked("git init -b master", { cwd: repoRoot });
  await runChecked("git config user.name 'Test User'", { cwd: repoRoot });
  await runChecked("git config user.email 'test@example.com'", { cwd: repoRoot });
  await writeFile(join(repoRoot, "README.md"), "# Test\n", "utf8");
  await writeFile(join(repoRoot, ".gitignore"), ".ai-runs/\n", "utf8");
  await runChecked("git add README.md .gitignore", { cwd: repoRoot });
  await runChecked("git commit -m 'chore: init'", { cwd: repoRoot });
  return repoRoot;
}

function makeTask(overrides?: Partial<TaskRecord>): TaskRecord {
  return {
    id: "TASK-001",
    title: "Implement login",
    description: "Add login flow for MVP.",
    priority: "P1",
    status: "ready",
    depends_on: [],
    acceptance_criteria: ["Login screen exists"],
    constraints: [],
    expected_files: [],
    ...overrides,
  };
}

function makeArchitectOutput(overrides?: Partial<ArchitectOutput>): ArchitectOutput {
  return {
    principles: ["Keep UI logic simple and testable"],
    moduleConstraints: ["Confine changes to app module"],
    dependencyRules: ["No new dependency without ADR"],
    requiredAdrs: [],
    risks: [],
    architectureNotes: "Follow existing Android architecture conventions.",
    adrs: [],
    ...overrides,
  };
}

function makeImplementerOutput(overrides?: Partial<ImplementerOutput>): ImplementerOutput {
  return {
    commits: [],
    touchedFiles: [],
    unresolvedRisks: [],
    notes: [],
    ...overrides,
  };
}

function makeVerifierOutput(overrides?: Partial<VerifierOutput>): VerifierOutput {
  return {
    checks: [":app:testDebugUnitTest", ":app:lintDebug", ":app:assembleDebug"],
    passed: true,
    failures: [],
    repairHints: [],
    ...overrides,
  };
}

describe("TaskOrchestrator", () => {
  let repoRoot = "";

  beforeEach(async () => {
    repoRoot = await createTempRepo();
  });

  afterEach(async () => {
    if (repoRoot) {
      await rm(repoRoot, { recursive: true, force: true });
    }
  });

  it("sync-plan writes planner tasks into TASKS.yml", async () => {
    const plannerTasks = [
      makeTask({ id: "TASK-001", title: "First", priority: "P1", status: "ready" }),
      makeTask({ id: "TASK-002", title: "Second", priority: "P2", status: "backlog" }),
    ];

    const fake = new FakeAgentRunner({
      planner: {
        summary: "Planned from PRD",
        blocked: false,
        questions: [],
        tasks: plannerTasks,
      },
      architect: [makeArchitectOutput()],
      implementer: [makeImplementerOutput()],
      verifier: [makeVerifierOutput()],
    });

    const orchestrator = new TaskOrchestrator(repoRoot, {
      createAgentRunner: () => fake,
      runPreflight: async () => ({
        originUrl: "",
        gitUserName: "Test User",
        gitUserEmail: "test@example.com",
      }),
      runVerification: async () => {
        throw new Error("verification should not run in sync-plan");
      },
    });

    const result = await orchestrator.execute({
      mode: "sync-plan",
      maxIterations: 8,
      repairAttempts: 1,
      baseBranch: "master",
      noPr: true,
      draftPr: false,
    });

    expect(result.exitCode).toBe(0);
    expect(fake.calls).toEqual(["planner"]);

    const tasksRaw = await readFile(join(repoRoot, "TASKS.yml"), "utf8");
    const parsed = YAML.parse(tasksRaw) as { tasks: Array<{ id: string }> };
    expect(parsed.tasks.map((task) => task.id)).toEqual(["TASK-001", "TASK-002"]);
  });

  it("returns exit code 3 when planner is blocked in execute-next", async () => {
    const fake = new FakeAgentRunner({
      planner: {
        summary: "Need product clarification",
        blocked: true,
        questions: ["Which auth provider should we use?"],
        tasks: [],
      },
      architect: [makeArchitectOutput()],
      implementer: [makeImplementerOutput()],
      verifier: [makeVerifierOutput()],
    });

    const orchestrator = new TaskOrchestrator(repoRoot, {
      createAgentRunner: () => fake,
      runPreflight: async () => ({
        originUrl: "git@example.com/repo.git",
        gitUserName: "Test User",
        gitUserEmail: "test@example.com",
      }),
      runVerification: async () => {
        throw new Error("verification should not run for blocked planner");
      },
    });

    const result = await orchestrator.execute({
      mode: "execute-next",
      maxIterations: 8,
      repairAttempts: 1,
      baseBranch: "master",
      noPr: true,
      draftPr: false,
    });

    expect(result.exitCode).toBe(3);
    expect(fake.calls).toEqual(["planner"]);
    expect(result.summary).toContain("Planner blocked");
  });

  it("executes repair loop and passes architect guidance to implementer", async () => {
    const task = makeTask({ id: "TASK-010", title: "Login screen", status: "ready" });
    const fake = new FakeAgentRunner({
      planner: {
        summary: "Ready to execute one task",
        blocked: false,
        questions: [],
        tasks: [task],
      },
      architect: [makeArchitectOutput()],
      implementer: [
        makeImplementerOutput({ notes: ["Initial implementation"] }),
        makeImplementerOutput({ notes: ["Repair implementation"] }),
      ],
      verifier: [
        makeVerifierOutput({ passed: false, failures: ["lint failed"], repairHints: ["Fix lint"] }),
        makeVerifierOutput({ passed: true }),
      ],
    });

    let verificationAttempt = 0;
    const orchestrator = new TaskOrchestrator(repoRoot, {
      createAgentRunner: () => fake,
      runPreflight: async () => ({
        originUrl: "git@example.com/repo.git",
        gitUserName: "Test User",
        gitUserEmail: "test@example.com",
      }),
      runVerification: async (
        _cwd: string,
        _level: "quick" | "full",
        writeLog: (fileName: string, body: string) => Promise<string>,
      ): Promise<VerificationReport> => {
        verificationAttempt += 1;
        if (verificationAttempt === 1) {
          const logsPath = await writeLog("verification.log", "lint failed");
          return {
            checks: ["./gradlew :app:testDebugUnitTest", "./gradlew :app:lintDebug", "./gradlew :app:assembleDebug"],
            passed: false,
            failedStep: "./gradlew :app:lintDebug",
            logsPath,
          };
        }

        const logsPath = await writeLog("verification.log", "all checks passed");
        return {
          checks: ["./gradlew :app:testDebugUnitTest", "./gradlew :app:lintDebug", "./gradlew :app:assembleDebug"],
          passed: true,
          failedStep: null,
          logsPath,
        };
      },
    });

    const result = await orchestrator.execute({
      mode: "execute-next",
      maxIterations: 8,
      repairAttempts: 1,
      baseBranch: "master",
      noPr: true,
      draftPr: false,
    });

    expect(result.exitCode).toBe(0);
    expect(verificationAttempt).toBe(2);
    expect(fake.calls).toEqual(["planner", "architect", "implementer", "verifier", "implementer", "verifier"]);
    expect(fake.implementerInputs[0].architect.principles).toContain("Keep UI logic simple and testable");
    expect(fake.implementerInputs[1].verificationFailure).toBe("./gradlew :app:lintDebug");

    const tasksRaw = await readFile(join(repoRoot, "TASKS.yml"), "utf8");
    const parsed = YAML.parse(tasksRaw) as { tasks: Array<{ id: string; status: string }> };
    expect(parsed.tasks.find((entry) => entry.id === "TASK-010")?.status).toBe("review");
  });

  it("returns exit code 2 when checks still fail after repair attempts", async () => {
    const task = makeTask({ id: "TASK-020", title: "Profile screen", status: "ready" });
    const fake = new FakeAgentRunner({
      planner: {
        summary: "Ready",
        blocked: false,
        questions: [],
        tasks: [task],
      },
      architect: [makeArchitectOutput()],
      implementer: [
        makeImplementerOutput({ notes: ["Attempt 1"] }),
        makeImplementerOutput({ notes: ["Attempt 2"] }),
      ],
      verifier: [makeVerifierOutput({ passed: false, failures: ["assemble failed"], repairHints: ["Fix compile"] })],
    });

    const orchestrator = new TaskOrchestrator(repoRoot, {
      createAgentRunner: () => fake,
      runPreflight: async () => ({
        originUrl: "git@example.com/repo.git",
        gitUserName: "Test User",
        gitUserEmail: "test@example.com",
      }),
      runVerification: async (
        _cwd: string,
        _level: "quick" | "full",
        writeLog: (fileName: string, body: string) => Promise<string>,
      ): Promise<VerificationReport> => {
        const logsPath = await writeLog("verification.log", "still failing");
        return {
          checks: ["./gradlew :app:testDebugUnitTest", "./gradlew :app:lintDebug", "./gradlew :app:assembleDebug"],
          passed: false,
          failedStep: "./gradlew :app:assembleDebug",
          logsPath,
        };
      },
    });

    const result = await orchestrator.execute({
      mode: "execute-next",
      maxIterations: 8,
      repairAttempts: 1,
      baseBranch: "master",
      noPr: true,
      draftPr: false,
    });

    expect(result.exitCode).toBe(2);
    expect(fake.calls).toEqual(["planner", "architect", "implementer", "verifier", "implementer", "verifier"]);

    const tasksRaw = await readFile(join(repoRoot, "TASKS.yml"), "utf8");
    const parsed = YAML.parse(tasksRaw) as { tasks: Array<{ id: string; status: string }> };
    expect(parsed.tasks.find((entry) => entry.id === "TASK-020")?.status).toBe("review");
  });
});
