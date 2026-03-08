import type { ExecutionState } from "../types";

function unique(items: string[]): string[] {
  return [...new Set(items.filter((item) => item.trim().length > 0))];
}

export function createPrTitle(taskId: string, taskTitle: string): string {
  const concise = taskTitle.trim().replace(/\s+/g, " ").slice(0, 64);
  return `${taskId}: ${concise.length > 0 ? concise : "Automated task update"}`;
}

export function createPrBody(state: ExecutionState): string {
  const planner = state.outputs.planner;
  const architect = state.outputs.architect;
  const implementer = state.outputs.implementer;
  const verifier = state.outputs.verifier;

  const files = unique(implementer?.touchedFiles ?? []);
  const commits = unique(implementer?.commits ?? []);
  const taskId = state.selectedTaskId ?? "unknown";

  const sections = [
    "## Task",
    `- ID: ${taskId}`,
    "",
    "## Planner Summary",
    planner?.summary ?? "Not provided",
    "",
    "## Architect Decisions",
    ...(architect?.principles?.length ? architect.principles.map((line) => `- ${line}`) : ["- Not provided"]),
    "",
    "## Module Constraints",
    ...(architect?.moduleConstraints?.length
      ? architect.moduleConstraints.map((line) => `- ${line}`)
      : ["- None recorded"]),
    "",
    "## Dependency Rules",
    ...(architect?.dependencyRules?.length
      ? architect.dependencyRules.map((line) => `- ${line}`)
      : ["- None recorded"]),
    "",
    "## Key Files",
    ...(files.length > 0 ? files.map((file) => `- ${file}`) : ["- No file changes recorded"]),
    "",
    "## Validation",
    ...(state.verification
      ? [
          `- Checks: ${state.verification.checks.join(", ")}`,
          `- Passed: ${state.verification.passed ? "yes" : "no"}`,
          `- Failed step: ${state.verification.failedStep ?? "none"}`,
        ]
      : ["- Validation did not run"]),
    "",
    "## Governance Audit",
    ...(state.governanceAudit
      ? [
          `- Passed: ${state.governanceAudit.passed ? "yes" : "no"}`,
          `- ADR required: ${state.governanceAudit.adrRequired ? "yes" : "no"}`,
          ...(state.governanceAudit.strategicViolations.length > 0
            ? state.governanceAudit.strategicViolations.map((line) => `- Violation: ${line}`)
            : ["- No strategic violations"]),
        ]
      : ["- Governance audit did not run"]),
    "",
    "## Commits",
    ...(commits.length > 0 ? commits.map((message) => `- ${message}`) : ["- No commits recorded"]),
    "",
    "## Verifier Notes",
    ...(verifier?.repairHints?.length ? verifier.repairHints.map((line) => `- ${line}`) : ["- None"]),
  ];

  return `${sections.join("\n")}\n`;
}
