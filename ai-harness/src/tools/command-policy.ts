const HARD_DENY_PATTERNS: RegExp[] = [
  /\bgit\s+reset\s+--hard\b/i,
  /\bgit\s+clean\s+-fdx\b/i,
  /\brm\s+-rf\s+\//i,
  /\bsudo\b/i,
  /\bchmod\s+-R\s+777\b/i,
  /\bgit\s+push\s+--force\b/i,
  /\bgit\s+push\s+-f\b/i,
];

export function assertCommandAllowed(command: string): void {
  for (const pattern of HARD_DENY_PATTERNS) {
    if (pattern.test(command)) {
      throw new Error(`Blocked command by policy: ${command}`);
    }
  }
}

export function listDenylist(): string[] {
  return HARD_DENY_PATTERNS.map((pattern) => pattern.source);
}
