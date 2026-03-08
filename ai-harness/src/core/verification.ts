import { resolve } from "node:path";

import type { CheckLevel, VerificationReport } from "../types";
import { runCommand } from "../tools/command-runner";

export function verificationCommands(level: CheckLevel): string[] {
  if (level === "quick") {
    return ["./gradlew :app:testDebugUnitTest"];
  }
  return [
    "./gradlew :app:testDebugUnitTest",
    "./gradlew :app:lintDebug",
    "./gradlew :app:assembleDebug",
  ];
}

export async function runVerification(
  repoRoot: string,
  level: CheckLevel,
  writeLog: (fileName: string, body: string) => Promise<string>,
): Promise<VerificationReport> {
  const checks = verificationCommands(level);
  const chunks: string[] = [];

  for (const check of checks) {
    const result = await runCommand(check, {
      cwd: repoRoot,
      timeoutMs: 25 * 60_000,
    });

    chunks.push([
      `## ${check}`,
      "",
      `exitCode: ${result.exitCode}`,
      "",
      "stdout:",
      result.stdout || "<empty>",
      "",
      "stderr:",
      result.stderr || "<empty>",
      "",
    ].join("\n"));

    if (result.exitCode !== 0) {
      const logsPath = await writeLog("verification.log", chunks.join("\n"));
      return {
        checks,
        passed: false,
        failedStep: check,
        logsPath: resolve(logsPath),
      };
    }
  }

  const logsPath = await writeLog("verification.log", chunks.join("\n"));
  return {
    checks,
    passed: true,
    failedStep: null,
    logsPath: resolve(logsPath),
  };
}
