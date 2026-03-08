import { runCommand } from "../tools/command-runner";
import type { PreflightResult } from "../types";
import { HarnessError } from "../types";
import { getGitIdentity, getOriginUrl, hasOriginRemote } from "./git";

export interface PreflightOptions {
  requireOrigin: boolean;
  requireGhAuth: boolean;
  requireOpenAI: boolean;
}

export async function runPreflight(repoRoot: string, options: PreflightOptions): Promise<PreflightResult> {
  if (options.requireOrigin) {
    const hasOrigin = await hasOriginRemote({ cwd: repoRoot });
    if (!hasOrigin) {
      throw new HarnessError(
        "Missing git remote 'origin'. Add an origin before running autonomous PR flow.",
        4,
      );
    }
  }

  if (options.requireOpenAI && !process.env.OPENAI_API_KEY) {
    throw new HarnessError("Missing OPENAI_API_KEY. Set it in your environment before running.", 4);
  }

  if (options.requireGhAuth) {
    const ghAuth = await runCommand("gh auth status", { cwd: repoRoot });
    if (ghAuth.exitCode !== 0) {
      throw new HarnessError(`GitHub CLI is not authenticated.\n${ghAuth.stderr || ghAuth.stdout}`, 4);
    }
  }

  const [originUrl, identity] = await Promise.all([
    options.requireOrigin ? getOriginUrl({ cwd: repoRoot }) : Promise.resolve(""),
    getGitIdentity({ cwd: repoRoot }),
  ]);

  return {
    originUrl,
    gitUserName: identity.name,
    gitUserEmail: identity.email,
  };
}
