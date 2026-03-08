import { z } from "zod";

export const runModeSchema = z.enum(["sync-plan", "execute-next", "autopilot", "reconcile"]);
export type RunMode = z.infer<typeof runModeSchema>;

export const checkLevelSchema = z.enum(["quick", "full"]);
export type CheckLevel = z.infer<typeof checkLevelSchema>;

export const taskStatusSchema = z.enum(["backlog", "ready", "in_progress", "review", "done", "blocked"]);
export type TaskStatus = z.infer<typeof taskStatusSchema>;

export const prioritySchema = z.enum(["P0", "P1", "P2", "P3"]);
export type TaskPriority = z.infer<typeof prioritySchema>;

export const taskRecordSchema = z.object({
  id: z.string().min(1),
  title: z.string().min(1),
  description: z.string().min(1),
  priority: prioritySchema,
  status: taskStatusSchema,
  depends_on: z.array(z.string()).default([]),
  acceptance_criteria: z.array(z.string()).min(1),
  constraints: z.array(z.string()).default([]),
  expected_files: z.array(z.string()).default([]),
  blocked_reason: z.string().optional(),
  pr_url: z.string().optional(),
  owner_role: z.enum(["planner", "architect", "implementer", "verifier"]).optional(),
});

export type TaskRecord = z.infer<typeof taskRecordSchema>;

export const tasksFileSchema = z.object({
  version: z.literal(1).default(1),
  tasks: z.array(taskRecordSchema),
});

export type TasksFile = z.infer<typeof tasksFileSchema>;

export const plannerOutputSchema = z.object({
  summary: z.string().min(1),
  blocked: z.boolean().default(false),
  questions: z.array(z.string()).default([]),
  tasks: z.array(taskRecordSchema),
});

export type PlannerOutput = z.infer<typeof plannerOutputSchema>;

export const adrDraftSchema = z.object({
  id: z.string().min(1),
  title: z.string().min(1),
  status: z.enum(["proposed", "accepted", "deprecated"]).default("accepted"),
  context: z.string().min(1),
  decision: z.string().min(1),
  consequences: z.array(z.string()).default([]),
});

export type AdrDraft = z.infer<typeof adrDraftSchema>;

export const architectOutputSchema = z.object({
  principles: z.array(z.string()).min(1),
  moduleConstraints: z.array(z.string()).default([]),
  dependencyRules: z.array(z.string()).default([]),
  requiredAdrs: z.array(z.string()).default([]),
  risks: z.array(z.string()).default([]),
  architectureNotes: z.string().default(""),
  adrs: z.array(adrDraftSchema).default([]),
});

export type ArchitectOutput = z.infer<typeof architectOutputSchema>;

export const implementerOutputSchema = z.object({
  commits: z.array(z.string()).default([]),
  touchedFiles: z.array(z.string()).default([]),
  unresolvedRisks: z.array(z.string()).default([]),
  notes: z.array(z.string()).default([]),
});

export type ImplementerOutput = z.infer<typeof implementerOutputSchema>;

export const verifierOutputSchema = z.object({
  checks: z.array(z.string()).min(1),
  passed: z.boolean(),
  failures: z.array(z.string()).default([]),
  repairHints: z.array(z.string()).default([]),
});

export type VerifierOutput = z.infer<typeof verifierOutputSchema>;

export const governanceAuditReportSchema = z.object({
  passed: z.boolean(),
  strategicViolations: z.array(z.string()).default([]),
  adrRequired: z.boolean().default(false),
  notes: z.array(z.string()).default([]),
});

export type GovernanceAuditReport = z.infer<typeof governanceAuditReportSchema>;

export const verificationReportSchema = z.object({
  checks: z.array(z.string()).min(1),
  passed: z.boolean(),
  failedStep: z.string().nullable(),
  logsPath: z.string(),
});

export type VerificationReport = z.infer<typeof verificationReportSchema>;

export const harnessRequestSchema = z.object({
  mode: runModeSchema,
  taskId: z.string().optional(),
  maxIterations: z.number().int().min(1).max(30).default(8),
  repairAttempts: z.number().int().min(0).max(10).default(3),
  baseBranch: z.string().min(1).default("master"),
  noPr: z.boolean().default(false),
  draftPr: z.boolean().default(false),
});

export type HarnessRequest = z.infer<typeof harnessRequestSchema>;

export interface RunArtifactIndex {
  runId: string;
  startedAt: string;
  finishedAt?: string;
  outcome?: "success" | "pr_with_failures" | "blocked" | "failed" | "noop";
  branchName?: string;
  selectedTaskId?: string;
  commitShas: string[];
  prUrl?: string;
  artifactPaths: Record<string, string>;
}

export interface TaskTransition {
  taskId: string;
  from: TaskStatus;
  to: TaskStatus;
  reason: string;
  timestamp: string;
}

export interface PreflightResult {
  originUrl: string;
  gitUserName: string;
  gitUserEmail: string;
}

export interface OrchestratorResult {
  exitCode: 0 | 2 | 3 | 4;
  runId: string;
  branchName?: string;
  prUrl?: string;
  summary: string;
}

export interface AgentOutputs {
  planner?: PlannerOutput;
  architect?: ArchitectOutput;
  implementer?: ImplementerOutput;
  verifier?: VerifierOutput;
}

export interface ExecutionState {
  request: HarnessRequest;
  runId: string;
  branchName?: string;
  branchCreatedAt?: string;
  selectedTaskId?: string;
  commitShas: string[];
  outputs: AgentOutputs;
  verification?: VerificationReport;
  governanceAudit?: GovernanceAuditReport;
  taskTransitions: TaskTransition[];
  docDiffSummary: string[];
  prUrl?: string;
  repairLoopCount: number;
}

export interface CommandResult {
  command: string;
  stdout: string;
  stderr: string;
  exitCode: number;
  durationMs: number;
}

export interface GovernanceDocs {
  prd: string;
  architecture: string;
  tasks: TasksFile;
  adrs: Record<string, string>;
}

export class HarnessError extends Error {
  public readonly code: 3 | 4;

  constructor(message: string, code: 3 | 4 = 4) {
    super(message);
    this.code = code;
    this.name = "HarnessError";
  }
}
