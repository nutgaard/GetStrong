import { describe, expect, it } from "bun:test";
import type { RunStreamEvent } from "@openai/agents";

import { extractReasoningSnippet, normalizeAgentStreamEvent } from "../../src/agents/openai-agent-runner";

function asEvent(value: unknown): RunStreamEvent {
  return value as RunStreamEvent;
}

describe("stream event normalization", () => {
  it("maps raw response lifecycle events", () => {
    const started = normalizeAgentStreamEvent(
      "planner",
      asEvent({ type: "raw_model_stream_event", data: { type: "response_started" } }),
    );
    const done = normalizeAgentStreamEvent(
      "planner",
      asEvent({
        type: "raw_model_stream_event",
        data: {
          type: "response_done",
          response: {
            usage: {
              totalTokens: 42,
              inputTokens: 30,
              outputTokens: 12,
            },
          },
        },
      }),
    );

    expect(started).toEqual([{ kind: "agent.planner.model.response_started", payload: {} }]);
    expect(done).toEqual([
      {
        kind: "agent.planner.model.response_done",
        payload: {
          totalTokens: 42,
          inputTokens: 30,
          outputTokens: 12,
        },
      },
    ]);
  });

  it("suppresses output_text_delta stream spam", () => {
    const events = normalizeAgentStreamEvent(
      "planner",
      asEvent({ type: "raw_model_stream_event", data: { type: "output_text_delta", delta: "x" } }),
    );

    expect(events).toEqual([]);
  });

  it("maps reasoning/tool/handoff run item events", () => {
    const reasoning = normalizeAgentStreamEvent(
      "architect",
      asEvent({
        type: "run_item_stream_event",
        name: "reasoning_item_created",
        item: {
          rawItem: {
            type: "reasoning",
            rawContent: [{ type: "reasoning_text", text: "derive boundaries and dependency constraints" }],
          },
        },
      }),
    );

    const toolCalled = normalizeAgentStreamEvent(
      "implementer",
      asEvent({
        type: "run_item_stream_event",
        name: "tool_called",
        item: {
          rawItem: {
            type: "function_call",
            name: "read_repo_file",
            callId: "call_123",
          },
        },
      }),
    );

    const toolOutput = normalizeAgentStreamEvent(
      "implementer",
      asEvent({
        type: "run_item_stream_event",
        name: "tool_output",
        item: {
          rawItem: {
            type: "function_call_result",
            name: "read_repo_file",
            callId: "call_123",
          },
        },
      }),
    );

    const handoff = normalizeAgentStreamEvent(
      "planner",
      asEvent({
        type: "run_item_stream_event",
        name: "handoff_occurred",
        item: {},
      }),
    );

    expect(reasoning[0]?.kind).toBe("agent.architect.reasoning_item_created");
    expect(String(reasoning[0]?.payload.snippet)).toContain("boundaries");

    expect(toolCalled).toEqual([
      {
        kind: "agent.implementer.tool_called",
        payload: {
          toolType: "function_call",
          name: "read_repo_file",
          callId: "call_123",
        },
      },
    ]);

    expect(toolOutput).toEqual([
      {
        kind: "agent.implementer.tool_output",
        payload: {
          toolType: "function_call_result",
          name: "read_repo_file",
          callId: "call_123",
        },
      },
    ]);

    expect(handoff).toEqual([{ kind: "agent.planner.handoff_occurred", payload: {} }]);
  });

  it("extracts and truncates reasoning snippets", () => {
    const snippet = extractReasoningSnippet({
      rawItem: {
        rawContent: [{ text: "a".repeat(300) }],
      },
    });

    expect(snippet?.length).toBeLessThanOrEqual(163);
    expect(snippet?.endsWith("...")).toBe(true);
  });
});
