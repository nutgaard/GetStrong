import { resolve } from "node:path";

export function prdPath(repoRoot: string): string {
  return resolve(repoRoot, "PRD.md");
}

export function architecturePath(repoRoot: string): string {
  return resolve(repoRoot, "ARCHITECTURE.md");
}

export function tasksPath(repoRoot: string): string {
  return resolve(repoRoot, "TASKS.yml");
}

export function adrsDir(repoRoot: string): string {
  return resolve(repoRoot, "docs", "ADRs");
}

export function isDocFile(path: string): boolean {
  return (
    path === "PRD.md" ||
    path === "ARCHITECTURE.md" ||
    path === "TASKS.yml" ||
    path.startsWith("docs/ADRs/")
  );
}

export const DEFAULT_PRD_TEMPLATE = `# Product Requirements Document\n\n## Goal\nDescribe the product goal and why this app exists.\n\n## Users\nDescribe the primary users and their needs.\n\n## MVP Scope\n-\n\n## Out Of Scope\n-\n\n## User Stories\n- As a ..., I want ..., so that ...\n\n## Acceptance Criteria\n-\n`;

export const DEFAULT_ARCHITECTURE_TEMPLATE = `# Architecture\n\n## Principles\n-\n\n## Module Boundaries\n-\n\n## Dependency Rules\n-\n\n## Implementation Notes\n-\n`;

export const DEFAULT_TASKS_TEMPLATE = `version: 1\ntasks: []\n`;
