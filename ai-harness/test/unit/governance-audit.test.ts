import { describe, expect, it } from "bun:test";

import { auditStrategicAdherence } from "../../src/governance/audit";
import type { ArchitectOutput, TaskRecord } from "../../src/types";

function makeTask(overrides?: Partial<TaskRecord>): TaskRecord {
  return {
    id: "TASK-001",
    title: "Task",
    description: "Description",
    priority: "P1",
    status: "ready",
    depends_on: [],
    acceptance_criteria: ["done"],
    constraints: [],
    expected_files: ["app/src/main/java/MainActivity.kt"],
    ...overrides,
  };
}

function makeArchitect(overrides?: Partial<ArchitectOutput>): ArchitectOutput {
  return {
    principles: ["Preserve module boundaries"],
    moduleConstraints: ["No cross-module leakage"],
    dependencyRules: ["No new dependencies without ADR"],
    requiredAdrs: [],
    risks: [],
    architectureNotes: "",
    adrs: [],
    ...overrides,
  };
}

describe("governance audit", () => {
  it("passes when strategic constraints are respected", () => {
    const report = auditStrategicAdherence({
      task: makeTask(),
      architect: makeArchitect(),
      changedFiles: ["app/src/main/java/MainActivity.kt", "TASKS.yml"],
    });

    expect(report.passed).toBe(true);
    expect(report.strategicViolations).toEqual([]);
  });

  it("fails when task changes files outside expected scope", () => {
    const report = auditStrategicAdherence({
      task: makeTask({ expected_files: ["app/src/main/java/MainActivity.kt"] }),
      architect: makeArchitect(),
      changedFiles: ["app/src/main/java/MainActivity.kt", "app/src/main/java/OtherScreen.kt"],
    });

    expect(report.passed).toBe(false);
    expect(report.strategicViolations.join(" ")).toContain("outside expected scope");
  });

  it("fails when dependency files change without ADR update", () => {
    const report = auditStrategicAdherence({
      task: makeTask({ expected_files: [] }),
      architect: makeArchitect(),
      changedFiles: ["ai-harness/package.json"],
    });

    expect(report.passed).toBe(false);
    expect(report.strategicViolations).toContain("Dependency-related files changed without ADR update in docs/ADRs.");
  });

  it("fails when architect requires ADR but none updated", () => {
    const report = auditStrategicAdherence({
      task: makeTask({ expected_files: [] }),
      architect: makeArchitect({ requiredAdrs: ["ADR-001"] }),
      changedFiles: ["app/src/main/java/MainActivity.kt"],
    });

    expect(report.passed).toBe(false);
    expect(report.strategicViolations.join(" ")).toContain("Architect required ADR updates");
  });
});
