#!/usr/bin/env bun

import "dotenv/config";

import { parseCli } from "./cli";
import { TaskOrchestrator } from "./core/orchestrator";
import { runChecked } from "./tools/command-runner";

async function main(): Promise<void> {
  const { request } = parseCli(process.argv.slice(2));

  const repoRootResult = await runChecked("git rev-parse --show-toplevel", { cwd: process.cwd() });
  const repoRoot = repoRootResult.stdout.trim();
  const orchestrator = new TaskOrchestrator(repoRoot);
  const result = await orchestrator.execute(request);

  if (result.prUrl) {
    console.log(`PR URL: ${result.prUrl}`);
  }
  console.log(`Run ID: ${result.runId}`);
  console.log(`Exit code: ${result.exitCode}`);
  console.log(result.summary);

  process.exitCode = result.exitCode;
}

main().catch((error) => {
  const message = error instanceof Error ? error.stack || error.message : String(error);
  console.error(message);
  process.exitCode = 4;
});
