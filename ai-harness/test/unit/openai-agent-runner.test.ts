import { describe, expect, it } from "bun:test";

import { OpenAIAgentRunner } from "../../src/agents/openai-agent-runner";
import { RunLogger } from "../../src/io/logger";

type InternalAgent = {
  model: string;
  modelSettings?: {
    reasoning?: {
      effort?: string;
    };
  };
};

describe("OpenAIAgentRunner model wiring", () => {
  it("uses hard-coded per-agent models with reasoning effort", () => {
    const logger = new RunLogger("test-run", process.cwd());
    const runner = new OpenAIAgentRunner({
      repoRoot: process.cwd(),
      logger,
    });

    const planner = (runner as unknown as { plannerAgent: InternalAgent }).plannerAgent;
    const architect = (runner as unknown as { architectAgent: InternalAgent }).architectAgent;
    const implementer = (runner as unknown as { implementerAgent: InternalAgent }).implementerAgent;
    const verifier = (runner as unknown as { verifierAgent: InternalAgent }).verifierAgent;

    expect(planner.model).toBe("gpt-5.1");
    expect(planner.modelSettings?.reasoning?.effort).toBe("medium");

    expect(architect.model).toBe("gpt-5.4");
    expect(architect.modelSettings?.reasoning?.effort).toBe("high");

    expect(implementer.model).toBe("gpt-5.3-codex");
    expect(implementer.modelSettings?.reasoning?.effort).toBe("medium");

    expect(verifier.model).toBe("gpt-5-mini");
    expect(verifier.modelSettings?.reasoning?.effort).toBe("low");
  });
});
