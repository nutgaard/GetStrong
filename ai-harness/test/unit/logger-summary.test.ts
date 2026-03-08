import { describe, expect, it } from "bun:test";

import { summarizePayload } from "../../src/io/logger";

describe("logger summarizePayload", () => {
  it("renders agent start/done states", () => {
    expect(summarizePayload("agent.planner.start", {})).toBe("started");

    const done = summarizePayload("agent.planner.done", {
      elapsedMs: 1234,
      taskCount: 23,
      passed: true,
    });

    expect(done).toContain("completed");
    expect(done).toContain("elapsedMs=1234");
    expect(done).toContain("tasks=23");
    expect(done).toContain("passed=yes");
  });

  it("renders model, reasoning, and tool milestones", () => {
    expect(summarizePayload("agent.planner.model.response_started", {})).toBe("model response started");
    expect(
      summarizePayload("agent.planner.model.response_done", {
        totalTokens: 88,
      }),
    ).toContain("tokens=88");

    expect(
      summarizePayload("agent.architect.reasoning_item_created", {
        snippet: "derive module boundaries",
      }),
    ).toContain("reasoning:");

    expect(
      summarizePayload("agent.implementer.tool_called", {
        name: "write_repo_file",
      }),
    ).toBe("tool called: write_repo_file");

    expect(
      summarizePayload("agent.implementer.tool_output", {
        toolType: "function_call_result",
      }),
    ).toBe("tool output: function_call_result");
  });

  it("passes through orchestrator errors", () => {
    const result = summarizePayload("orchestrator.error", {
      message: "failed to parse",
    });

    expect(result).toBe("failed to parse");
  });
});
