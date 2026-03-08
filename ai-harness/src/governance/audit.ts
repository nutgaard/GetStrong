import type { ArchitectOutput, GovernanceAuditReport, TaskRecord } from "../types";
import { governanceAuditReportSchema } from "../types";
import { isDocFile } from "./constants";

const DEPENDENCY_FILES = new Set([
  "package.json",
  "ai-harness/package.json",
  "app/build.gradle.kts",
  "build.gradle.kts",
  "gradle/libs.versions.toml",
]);

export function auditStrategicAdherence(input: {
  task: TaskRecord;
  architect: ArchitectOutput;
  changedFiles: string[];
}): GovernanceAuditReport {
  const violations: string[] = [];
  const notes: string[] = [];

  const changedCodeFiles = input.changedFiles.filter((file) => !isDocFile(file));

  if (input.task.expected_files.length > 0) {
    const allowed = new Set(input.task.expected_files);
    const outside = changedCodeFiles.filter((file) => !allowed.has(file));
    if (outside.length > 0) {
      violations.push(
        `Task changed files outside expected scope: ${outside.join(", ")}. Expected files are ${input.task.expected_files.join(
          ", ",
        )}.`,
      );
    }
  }

  const changedDependencyFile = input.changedFiles.some((file) => DEPENDENCY_FILES.has(file));
  const changedAdr = input.changedFiles.some((file) => file.startsWith("docs/ADRs/") && file.endsWith(".md"));
  if (changedDependencyFile && !changedAdr) {
    violations.push("Dependency-related files changed without ADR update in docs/ADRs.");
  }

  if (input.architect.requiredAdrs.length > 0 && !changedAdr) {
    violations.push(
      `Architect required ADR updates (${input.architect.requiredAdrs.join(", ")}) but no ADR files were changed.`,
    );
  }

  if (input.architect.dependencyRules.length > 0) {
    notes.push(`Dependency rules considered: ${input.architect.dependencyRules.join(" | ")}`);
  }

  if (input.architect.moduleConstraints.length > 0) {
    notes.push(`Module constraints considered: ${input.architect.moduleConstraints.join(" | ")}`);
  }

  const report = {
    passed: violations.length === 0,
    strategicViolations: violations,
    adrRequired: input.architect.requiredAdrs.length > 0,
    notes,
  };

  return governanceAuditReportSchema.parse(report);
}
