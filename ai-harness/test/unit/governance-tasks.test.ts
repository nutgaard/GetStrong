import { describe, expect, it } from "bun:test";

import { selectNextTask } from "../../src/governance/tasks";
import type { TaskRecord, TasksFile } from "../../src/types";

function makeTask(overrides?: Partial<TaskRecord>): TaskRecord {
  return {
    id: "TASK-001",
    title: "Task",
    description: "Description",
    priority: "P2",
    status: "ready",
    depends_on: [],
    acceptance_criteria: ["done"],
    constraints: [],
    expected_files: [],
    ...overrides,
  };
}

function makeTasksFile(tasks: TaskRecord[]): TasksFile {
  return {
    version: 1,
    tasks,
  };
}

describe("governance task selection", () => {
  it("selects highest-priority runnable task", () => {
    const tasks = makeTasksFile([
      makeTask({ id: "TASK-001", priority: "P2", status: "ready" }),
      makeTask({ id: "TASK-002", priority: "P0", status: "ready" }),
      makeTask({ id: "TASK-003", priority: "P1", status: "ready" }),
    ]);

    const selected = selectNextTask(tasks);
    expect(selected?.id).toBe("TASK-002");
  });

  it("skips task whose dependencies are not done", () => {
    const tasks = makeTasksFile([
      makeTask({ id: "TASK-001", status: "done" }),
      makeTask({ id: "TASK-002", status: "ready", depends_on: ["TASK-001"] }),
      makeTask({ id: "TASK-003", status: "ready", depends_on: ["TASK-999"], priority: "P0" }),
    ]);

    const selected = selectNextTask(tasks);
    expect(selected?.id).toBe("TASK-002");
  });

  it("throws when forced task is not ready", () => {
    const tasks = makeTasksFile([makeTask({ id: "TASK-010", status: "backlog" })]);

    expect(() => selectNextTask(tasks, "TASK-010")).toThrow("must be in 'ready' status");
  });
});
