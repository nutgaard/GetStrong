import { describe, expect, it } from "bun:test";

import { assertCommandAllowed } from "../../src/tools/command-policy";

describe("command policy", () => {
  it("allows ordinary git and gradle commands", () => {
    expect(() => assertCommandAllowed("git status")).not.toThrow();
    expect(() => assertCommandAllowed("./gradlew :app:assembleDebug")).not.toThrow();
  });

  it("blocks hard reset", () => {
    expect(() => assertCommandAllowed("git reset --hard HEAD~1")).toThrow();
  });

  it("blocks force push", () => {
    expect(() => assertCommandAllowed("git push --force origin test")).toThrow();
  });
});
