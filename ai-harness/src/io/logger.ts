import { appendFile, mkdir, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";

function nowIso(): string {
  return new Date().toISOString();
}

export class RunLogger {
  public readonly runId: string;
  public readonly runDir: string;
  private readonly eventsPath: string;
  private readonly streamToStdout: boolean;

  constructor(runId: string, rootDir: string, options?: { streamToStdout?: boolean }) {
    this.runId = runId;
    this.runDir = resolve(rootDir, ".ai-runs", runId);
    this.eventsPath = resolve(this.runDir, "events.jsonl");
    this.streamToStdout = options?.streamToStdout ?? true;
  }

  async init(): Promise<void> {
    await mkdir(this.runDir, { recursive: true });
  }

  async writeJson(fileName: string, value: unknown): Promise<string> {
    const outputPath = resolve(this.runDir, fileName);
    await mkdir(dirname(outputPath), { recursive: true });
    await writeFile(outputPath, `${JSON.stringify(value, null, 2)}\n`, "utf8");
    return outputPath;
  }

  async writeText(fileName: string, value: string): Promise<string> {
    const outputPath = resolve(this.runDir, fileName);
    await mkdir(dirname(outputPath), { recursive: true });
    await writeFile(outputPath, value, "utf8");
    return outputPath;
  }

  async event(kind: string, payload: unknown): Promise<void> {
    const line = JSON.stringify({ timestamp: nowIso(), kind, payload });
    await appendFile(this.eventsPath, `${line}\n`, "utf8");
    if (this.streamToStdout) {
      const suffix = summarizePayload(kind, payload);
      console.log(`[ai:${this.runId}] ${kind}${suffix ? ` | ${suffix}` : ""}`);
    }
  }
}

function summarizePayload(kind: string, payload: unknown): string {
  if (!payload || typeof payload !== "object") {
    return "";
  }

  const data = payload as Record<string, unknown>;

  if (kind === "phase.start" && typeof data.mode === "string") {
    return `mode=${data.mode}`;
  }

  if (kind === "phase.task.selected" && typeof data.taskId === "string") {
    const title = typeof data.title === "string" ? ` title="${data.title}"` : "";
    return `task=${data.taskId}${title}`;
  }

  if (kind === "phase.iteration.start" && typeof data.attempt === "number") {
    return `attempt=${data.attempt}`;
  }

  if (kind === "phase.verification.result") {
    const passed = typeof data.passed === "boolean" ? (data.passed ? "passed" : "failed") : "unknown";
    const step = typeof data.failedStep === "string" ? ` failedStep=${data.failedStep}` : "";
    return `${passed}${step}`;
  }

  if (kind === "phase.governance.result" && typeof data.passed === "boolean") {
    return data.passed ? "passed" : "failed";
  }

  if (kind === "phase.pr.opened" && typeof data.prUrl === "string") {
    return data.prUrl;
  }

  if (kind === "tool.run_repo_command") {
    const command = typeof data.command === "string" ? data.command : "";
    const exitCode = typeof data.exitCode === "number" ? ` exitCode=${data.exitCode}` : "";
    return `${command}${exitCode}`.trim();
  }

  if (kind === "tool.write_repo_file") {
    const path = typeof data.path === "string" ? data.path : "";
    const bytes = typeof data.bytes === "number" ? ` bytes=${data.bytes}` : "";
    return `${path}${bytes}`.trim();
  }

  if (kind === "tool.git_commit_all") {
    const message = typeof data.message === "string" ? `message="${data.message}"` : "";
    const sha = typeof data.sha === "string" && data.sha.length > 0 ? ` sha=${data.sha}` : "";
    return `${message}${sha}`.trim();
  }

  if (kind.startsWith("agent.")) {
    return "completed";
  }

  if (kind === "orchestrator.error" && typeof data.message === "string") {
    return data.message;
  }

  return "";
}

export function createRunId(): string {
  const d = new Date();
  const pad = (n: number): string => String(n).padStart(2, "0");
  const timestamp = `${d.getUTCFullYear()}${pad(d.getUTCMonth() + 1)}${pad(d.getUTCDate())}-${pad(
    d.getUTCHours(),
  )}${pad(d.getUTCMinutes())}${pad(d.getUTCSeconds())}`;
  const random = Math.random().toString(36).slice(2, 8);
  return `${timestamp}-${random}`;
}
