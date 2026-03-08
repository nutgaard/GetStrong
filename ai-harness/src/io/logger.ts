import { appendFile, mkdir, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";

function nowIso(): string {
  return new Date().toISOString();
}

export class RunLogger {
  public readonly runId: string;
  public readonly runDir: string;
  private readonly eventsPath: string;

  constructor(runId: string, rootDir: string) {
    this.runId = runId;
    this.runDir = resolve(rootDir, ".ai-runs", runId);
    this.eventsPath = resolve(this.runDir, "events.jsonl");
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
  }
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
