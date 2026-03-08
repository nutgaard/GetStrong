import type {
  ArchitectOutput,
  GovernanceDocs,
  ImplementerOutput,
  PlannerOutput,
  TaskRecord,
  VerifierOutput,
} from "../types";

export interface AgentRunner {
  runPlanner(input: { governance: GovernanceDocs }): Promise<PlannerOutput>;
  runArchitect(input: {
    task: TaskRecord;
    governance: GovernanceDocs;
    previousViolations?: string[];
  }): Promise<ArchitectOutput>;
  runImplementer(input: {
    task: TaskRecord;
    governance: GovernanceDocs;
    architect: ArchitectOutput;
    repairAttempt: number;
    verificationFailure?: string;
    verificationLogSnippet?: string;
    governanceViolations?: string[];
  }): Promise<ImplementerOutput>;
  runVerifier(input: {
    task: TaskRecord;
    checks: string[];
    passed: boolean;
    failedStep: string | null;
    verificationLogSnippet: string;
    implementer: ImplementerOutput;
  }): Promise<VerifierOutput>;
}
