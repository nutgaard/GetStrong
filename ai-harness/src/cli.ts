import { harnessRequestSchema, type HarnessRequest } from "./types";

export interface ParsedCli {
  request: HarnessRequest;
}

const MODES = new Set(["sync-plan", "execute-next", "autopilot", "reconcile"]);

export function parseCli(argv: string[]): ParsedCli {
  if (argv.length === 0) {
    throw new Error(
      "Missing mode. Usage: bun run ai -- <sync-plan|execute-next|autopilot|reconcile> [--task TASK-001] [--repair-attempts N] [--base-branch BRANCH] [--no-pr] [--draft-pr]",
    );
  }

  const first = argv[0];
  if (!MODES.has(first)) {
    throw new Error(`Invalid mode '${first}'. Expected one of: ${Array.from(MODES).join(", ")}`);
  }

  const flags: Record<string, string | boolean> = {};
  for (let i = 1; i < argv.length; i += 1) {
    const token = argv[i];
    if (!token.startsWith("--")) {
      throw new Error(`Unexpected positional argument: ${token}`);
    }

    const key = token.slice(2);
    if (["no-pr", "draft-pr"].includes(key)) {
      flags[key] = true;
      continue;
    }

    const next = argv[i + 1];
    if (!next || next.startsWith("--")) {
      throw new Error(`Missing value for --${key}`);
    }
    flags[key] = next;
    i += 1;
  }

  const request = harnessRequestSchema.parse({
    mode: first,
    taskId: toString(flags.task),
    maxIterations: toNumber(flags["max-iterations"], 8),
    repairAttempts: toNumber(flags["repair-attempts"], 3),
    baseBranch: toString(flags["base-branch"], "master"),
    noPr: Boolean(flags["no-pr"]),
    draftPr: Boolean(flags["draft-pr"]),
  });

  return { request };
}

function toNumber(value: string | boolean | undefined, fallback: number): number {
  if (value === undefined || typeof value === "boolean") {
    return fallback;
  }
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) {
    throw new Error(`Invalid numeric value: ${value}`);
  }
  return parsed;
}

function toString(value: string | boolean | undefined, fallback?: string): string | undefined {
  if (typeof value !== "string") {
    return fallback;
  }
  return value;
}
