import { resolve } from "node:path";

import { runChecked, runCommand } from "../tools/command-runner";

function trim(value: string): string {
  return value.trim();
}

export interface GitContext {
  cwd: string;
}

export async function hasOriginRemote(ctx: GitContext): Promise<boolean> {
  const result = await runCommand("git remote get-url origin", { cwd: ctx.cwd });
  return result.exitCode === 0 && trim(result.stdout).length > 0;
}

export async function getOriginUrl(ctx: GitContext): Promise<string> {
  const result = await runChecked("git remote get-url origin", { cwd: ctx.cwd });
  return trim(result.stdout);
}

export async function getGitIdentity(ctx: GitContext): Promise<{ name: string; email: string }> {
  const [nameResult, emailResult] = await Promise.all([
    runChecked("git config user.name", { cwd: ctx.cwd }),
    runChecked("git config user.email", { cwd: ctx.cwd }),
  ]);
  return {
    name: trim(nameResult.stdout),
    email: trim(emailResult.stdout),
  };
}

export async function ensureBaseBranch(ctx: GitContext, branch: string): Promise<void> {
  const verify = await runCommand(`git rev-parse --verify ${escapeShellArg(branch)}`, { cwd: ctx.cwd });
  if (verify.exitCode !== 0) {
    throw new Error(`Base branch not found locally: ${branch}`);
  }
}

export async function createTaskBranch(ctx: GitContext, baseBranch: string, branchName: string): Promise<void> {
  await runChecked(`git checkout ${escapeShellArg(baseBranch)}`, { cwd: ctx.cwd });
  await runChecked(`git checkout -b ${escapeShellArg(branchName)}`, { cwd: ctx.cwd });
}

export async function currentBranch(ctx: GitContext): Promise<string> {
  const result = await runChecked("git branch --show-current", { cwd: ctx.cwd });
  return trim(result.stdout);
}

export async function workingTreeClean(ctx: GitContext): Promise<boolean> {
  const result = await runChecked("git status --porcelain", { cwd: ctx.cwd });
  return trim(result.stdout).length === 0;
}

export async function listChangedFiles(ctx: GitContext): Promise<string[]> {
  const result = await runChecked("git status --porcelain", { cwd: ctx.cwd });
  return result.stdout
    .split("\n")
    .map((line: string) => line.trim())
    .filter((line: string) => line.length > 0)
    .map((line: string) => line.slice(3));
}

export interface CommitInfo {
  sha: string;
  message: string;
}

export async function listCommitsSince(ctx: GitContext, baseRef: string): Promise<CommitInfo[]> {
  const result = await runChecked(
    `git log --format=%H%x09%s ${escapeShellArg(baseRef)}..HEAD`,
    { cwd: ctx.cwd },
  );
  return result.stdout
    .split("\n")
    .map((line: string) => line.trim())
    .filter((line: string) => line.length > 0)
    .map((line: string) => {
      const [sha, ...rest] = line.split("\t");
      return {
        sha,
        message: rest.join("\t"),
      };
    });
}

export async function listFilesSince(ctx: GitContext, baseRef: string): Promise<string[]> {
  const result = await runChecked(`git diff --name-only ${escapeShellArg(baseRef)}..HEAD`, {
    cwd: ctx.cwd,
  });
  return result.stdout
    .split("\n")
    .map((line: string) => line.trim())
    .filter((line: string) => line.length > 0);
}

export async function commitAll(ctx: GitContext, message: string): Promise<string> {
  await runChecked("git add -A", { cwd: ctx.cwd });
  const commit = await runCommand(`git commit -m ${escapeShellArg(message)}`, { cwd: ctx.cwd });

  if (commit.exitCode !== 0) {
    if (commit.stdout.includes("nothing to commit") || commit.stderr.includes("nothing to commit")) {
      return "";
    }
    throw new Error(commit.stderr || commit.stdout);
  }

  const sha = await runChecked("git rev-parse HEAD", { cwd: ctx.cwd });
  return trim(sha.stdout);
}

export async function pushBranch(ctx: GitContext, branchName: string): Promise<void> {
  await runChecked(`git push -u origin ${escapeShellArg(branchName)}`, { cwd: ctx.cwd });
}

export async function openPr(
  ctx: GitContext,
  args: {
    baseBranch: string;
    headBranch: string;
    title: string;
    bodyPath: string;
    draft: boolean;
  },
): Promise<string> {
  const draftFlag = args.draft ? " --draft" : "";
  const command = `gh pr create --base ${escapeShellArg(args.baseBranch)} --head ${escapeShellArg(
    args.headBranch,
  )} --title ${escapeShellArg(args.title)} --body-file ${escapeShellArg(resolve(args.bodyPath))}${draftFlag}`;
  const result = await runChecked(command, { cwd: ctx.cwd });
  const lines = result.stdout
    .split("\n")
    .map((line: string) => line.trim())
    .filter((line: string) => line.startsWith("http"));

  if (lines.length === 0) {
    throw new Error(`Could not parse PR URL from output: ${result.stdout}`);
  }

  return lines[0];
}

export function createBranchName(taskId: string, title: string): string {
  const taskSlug = taskId
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 20) || "task";

  const titleSlug = title
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 24) || "work";

  const timestamp = new Date().toISOString().replace(/[-:TZ.]/g, "").slice(0, 14);
  return `codex/task-${taskSlug}-${titleSlug}-${timestamp}`;
}

function escapeShellArg(value: string): string {
  return `'${value.replace(/'/g, `'"'"'`)}'`;
}
