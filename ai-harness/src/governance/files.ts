import { mkdir, readFile, readdir, stat, writeFile } from "node:fs/promises";
import { basename, resolve } from "node:path";

import YAML from "yaml";

import {
  DEFAULT_ARCHITECTURE_TEMPLATE,
  DEFAULT_PRD_TEMPLATE,
  DEFAULT_TASKS_TEMPLATE,
  adrsDir,
  architecturePath,
  prdPath,
  tasksPath,
} from "./constants";
import { tasksFileSchema, type AdrDraft, type GovernanceDocs, type TasksFile } from "../types";

export async function ensureGovernanceScaffold(repoRoot: string): Promise<void> {
  const prd = prdPath(repoRoot);
  const architecture = architecturePath(repoRoot);
  const tasks = tasksPath(repoRoot);
  const adrDirectory = adrsDir(repoRoot);

  await mkdir(adrDirectory, { recursive: true });

  await ensureFile(prd, DEFAULT_PRD_TEMPLATE);
  await ensureFile(architecture, DEFAULT_ARCHITECTURE_TEMPLATE);
  await ensureFile(tasks, DEFAULT_TASKS_TEMPLATE);
}

async function ensureFile(path: string, content: string): Promise<void> {
  try {
    const fileStat = await stat(path);
    if (fileStat.isFile()) {
      return;
    }
  } catch {
    await writeFile(path, content, "utf8");
  }
}

export async function loadGovernanceDocs(repoRoot: string): Promise<GovernanceDocs> {
  const [prd, architecture, rawTasks] = await Promise.all([
    readFile(prdPath(repoRoot), "utf8"),
    readFile(architecturePath(repoRoot), "utf8"),
    readFile(tasksPath(repoRoot), "utf8"),
  ]);

  const parsedTasks = YAML.parse(rawTasks) as unknown;
  const tasks = tasksFileSchema.parse(parsedTasks);
  const adrs = await readAdrFiles(repoRoot);

  return {
    prd,
    architecture,
    tasks,
    adrs,
  };
}

export async function saveTasks(repoRoot: string, tasks: TasksFile): Promise<string> {
  const path = tasksPath(repoRoot);
  const serialized = YAML.stringify(tasksFileSchema.parse(tasks));
  await writeFile(path, serialized, "utf8");
  return path;
}

export async function saveArchitecture(repoRoot: string, content: string): Promise<string> {
  const path = architecturePath(repoRoot);
  await writeFile(path, `${content.trimEnd()}\n`, "utf8");
  return path;
}

export async function upsertAdrs(repoRoot: string, adrs: AdrDraft[]): Promise<string[]> {
  if (adrs.length === 0) {
    return [];
  }

  const directory = adrsDir(repoRoot);
  await mkdir(directory, { recursive: true });

  const written: string[] = [];
  for (const adr of adrs) {
    const fileName = `${slugifyAdrId(adr.id)}-${slugify(adr.title)}.md`;
    const outputPath = resolve(directory, fileName);
    const content = renderAdr(adr);
    await writeFile(outputPath, content, "utf8");
    written.push(outputPath);
  }

  return written;
}

function renderAdr(adr: AdrDraft): string {
  const consequences = adr.consequences.length > 0 ? adr.consequences : ["None recorded."];
  return [
    `# ${adr.id}: ${adr.title}`,
    "",
    `Status: ${adr.status}`,
    "",
    "## Context",
    adr.context.trim(),
    "",
    "## Decision",
    adr.decision.trim(),
    "",
    "## Consequences",
    ...consequences.map((line) => `- ${line}`),
    "",
  ].join("\n");
}

async function readAdrFiles(repoRoot: string): Promise<Record<string, string>> {
  const directory = adrsDir(repoRoot);
  try {
    const entries = await readdir(directory);
    const markdownFiles = entries.filter((entry) => entry.endsWith(".md"));
    const data = await Promise.all(
      markdownFiles.map(async (fileName) => {
        const fullPath = resolve(directory, fileName);
        const content = await readFile(fullPath, "utf8");
        return [basename(fullPath), content] as const;
      }),
    );

    return Object.fromEntries(data);
  } catch {
    return {};
  }
}

function slugify(value: string): string {
  return value
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 48) || "decision";
}

function slugifyAdrId(id: string): string {
  return id
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 24) || "adr";
}
