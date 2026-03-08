import { spawn } from "node:child_process";
import { performance } from "node:perf_hooks";
import { resolve } from "node:path";

import type { CommandResult } from "../types";
import { assertCommandAllowed } from "./command-policy";

export interface RunCommandOptions {
  cwd: string;
  timeoutMs?: number;
  env?: NodeJS.ProcessEnv;
}

export async function runCommand(command: string, options: RunCommandOptions): Promise<CommandResult> {
  assertCommandAllowed(command);

  const timeoutMs = options.timeoutMs ?? 10 * 60_000;
  const startedAt = performance.now();

  return await new Promise<CommandResult>((resolveResult, reject) => {
    const child = spawn(command, {
      cwd: resolve(options.cwd),
      env: {
        ...process.env,
        ...(options.env ?? {}),
      },
      shell: true,
      stdio: ["ignore", "pipe", "pipe"],
    });

    let stdout = "";
    let stderr = "";

    const timeout = setTimeout(() => {
      child.kill("SIGTERM");
      reject(new Error(`Command timed out after ${timeoutMs}ms: ${command}`));
    }, timeoutMs);

    child.stdout.on("data", (chunk) => {
      stdout += String(chunk);
    });

    child.stderr.on("data", (chunk) => {
      stderr += String(chunk);
    });

    child.on("error", (error) => {
      clearTimeout(timeout);
      reject(error);
    });

    child.on("close", (code) => {
      clearTimeout(timeout);
      const durationMs = performance.now() - startedAt;
      resolveResult({
        command,
        stdout,
        stderr,
        exitCode: code ?? 1,
        durationMs,
      });
    });
  });
}

export async function runChecked(command: string, options: RunCommandOptions): Promise<CommandResult> {
  const result = await runCommand(command, options);
  if (result.exitCode !== 0) {
    throw new Error(`Command failed (${result.exitCode}): ${command}\n${result.stderr || result.stdout}`);
  }
  return result;
}
