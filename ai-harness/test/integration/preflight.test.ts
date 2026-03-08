import { afterEach, describe, expect, it } from "bun:test";
import { mkdtemp, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

import { runPreflight } from "../../src/core/preflight";
import { runChecked } from "../../src/tools/command-runner";

const repos: string[] = [];

async function createRepo(withOrigin: boolean): Promise<string> {
  const repoRoot = await mkdtemp(join(tmpdir(), "getstrong-preflight-"));
  repos.push(repoRoot);

  await runChecked("git init -b master", { cwd: repoRoot });
  await runChecked("git config user.name 'Test User'", { cwd: repoRoot });
  await runChecked("git config user.email 'test@example.com'", { cwd: repoRoot });
  await writeFile(join(repoRoot, "README.md"), "# Test\n", "utf8");
  await runChecked("git add README.md", { cwd: repoRoot });
  await runChecked("git commit -m 'chore: init'", { cwd: repoRoot });

  if (withOrigin) {
    const bare = join(repoRoot, "origin.git");
    await runChecked(`git init --bare ${shellEscape(bare)}`, { cwd: repoRoot });
    await runChecked(`git remote add origin ${shellEscape(bare)}`, { cwd: repoRoot });
  }

  return repoRoot;
}

afterEach(async () => {
  while (repos.length > 0) {
    const repo = repos.pop();
    if (repo) {
      await rm(repo, { recursive: true, force: true });
    }
  }
});

describe("runPreflight", () => {
  it("fails fast when origin is missing", async () => {
    const repo = await createRepo(false);

    await expect(
      runPreflight(repo, {
        requireOrigin: true,
        requireGhAuth: false,
        requireOpenAI: false,
      }),
    ).rejects.toThrow("Missing git remote 'origin'");
  });

  it("fails fast when OPENAI_API_KEY is missing", async () => {
    const repo = await createRepo(true);
    const priorKey = process.env.OPENAI_API_KEY;
    delete process.env.OPENAI_API_KEY;

    await expect(
      runPreflight(repo, {
        requireOrigin: false,
        requireGhAuth: false,
        requireOpenAI: true,
      }),
    ).rejects.toThrow("Missing OPENAI_API_KEY");

    process.env.OPENAI_API_KEY = priorKey;
  });
});

function shellEscape(value: string): string {
  return `'${value.replace(/'/g, `'"'"'`)}'`;
}
