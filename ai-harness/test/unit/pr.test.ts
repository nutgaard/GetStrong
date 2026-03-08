import { describe, expect, it } from "bun:test";

import { createPrBody, createPrTitle } from "../../src/core/pr";
import type { ExecutionState } from "../../src/types";

function mockState(): ExecutionState {
  return {
    request: {
      mode: "execute-next",
      maxIterations: 8,
      repairAttempts: 3,
      baseBranch: "master",
      noPr: false,
      draftPr: false,
    },
    runId: "test-run",
    branchName: "codex/task-add-login-20260101",
    branchCreatedAt: "2026-01-01T00:00:00.000Z",
    selectedTaskId: "TASK-001",
    commitShas: ["abc123"],
    taskTransitions: [],
    docDiffSummary: ["ARCHITECTURE.md updated for TASK-001."],
    outputs: {
      planner: {
        summary: "Add login flow from PRD",
        blocked: false,
        questions: [],
        tasks: [
          {
            id: "TASK-001",
            title: "Add login flow",
            description: "Implement basic login flow.",
            priority: "P1",
            status: "ready",
            depends_on: [],
            acceptance_criteria: ["User can submit login form."],
            constraints: [],
            expected_files: ["app/src/main/java/MainActivity.kt"],
          },
        ],
      },
      architect: {
        principles: ["Prefer existing navigation graph"],
        moduleConstraints: ["UI remains in app module"],
        dependencyRules: ["Do not add new networking libraries"],
        requiredAdrs: [],
        risks: [],
        architectureNotes: "Keep changes scoped to MVP login journey.",
        adrs: [],
      },
      implementer: {
        commits: ["feat: add login screen"],
        touchedFiles: ["app/src/main/java/MainActivity.kt"],
        unresolvedRisks: [],
        notes: ["Implemented login screen and basic validation."],
      },
      verifier: {
        checks: [":app:testDebugUnitTest"],
        passed: true,
        failures: [],
        repairHints: [],
      },
    },
    verification: {
      checks: ["./gradlew :app:testDebugUnitTest"],
      passed: true,
      failedStep: null,
      logsPath: ".ai-runs/test-run/verification.log",
    },
    governanceAudit: {
      passed: true,
      strategicViolations: [],
      adrRequired: false,
      notes: [],
    },
    repairLoopCount: 0,
  };
}

describe("PR helpers", () => {
  it("builds a title from task id + title", () => {
    expect(createPrTitle("TASK-777", " Add better analytics event names ")).toBe("TASK-777: Add better analytics event names");
  });

  it("renders body sections", () => {
    const body = createPrBody(mockState());
    expect(body).toContain("## Task");
    expect(body).toContain("ID: TASK-001");
    expect(body).toContain("feat: add login screen");
    expect(body).toContain("app/src/main/java/MainActivity.kt");
    expect(body).toContain("Passed: yes");
    expect(body).toContain("## Architect Decisions");
    expect(body).toContain("## Governance Audit");
  });
});
