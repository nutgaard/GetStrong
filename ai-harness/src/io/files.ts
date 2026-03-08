import { readFile } from "node:fs/promises";

export async function readFileSafe(path: string): Promise<string> {
  try {
    return await readFile(path, "utf8");
  } catch {
    return "";
  }
}

export function tailSnippet(text: string, maxChars = 8_000): string {
  if (text.length <= maxChars) {
    return text;
  }
  return `...<truncated>\n${text.slice(-maxChars)}`;
}
