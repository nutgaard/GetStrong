import type { TaskPriority, TaskRecord, TaskStatus, TasksFile } from "../types";
import { HarnessError } from "../types";

const PRIORITY_ORDER: Record<TaskPriority, number> = {
  P0: 0,
  P1: 1,
  P2: 2,
  P3: 3,
};

export function selectNextTask(tasks: TasksFile, forcedTaskId?: string): TaskRecord | null {
  if (forcedTaskId) {
    const task = tasks.tasks.find((item) => item.id === forcedTaskId);
    if (!task) {
      throw new HarnessError(`Task ${forcedTaskId} not found in TASKS.yml`, 3);
    }
    return validateRunnableTask(task, tasks);
  }

  const runnable = tasks.tasks.filter((task) => task.status === "ready" && dependenciesDone(task, tasks));
  if (runnable.length === 0) {
    return null;
  }

  runnable.sort((a, b) => {
    const pa = PRIORITY_ORDER[a.priority];
    const pb = PRIORITY_ORDER[b.priority];
    if (pa !== pb) {
      return pa - pb;
    }
    return a.id.localeCompare(b.id);
  });

  return runnable[0];
}

function validateRunnableTask(task: TaskRecord, tasks: TasksFile): TaskRecord {
  if (task.status !== "ready") {
    throw new HarnessError(`Task ${task.id} must be in 'ready' status to execute.`, 3);
  }

  if (!dependenciesDone(task, tasks)) {
    throw new HarnessError(`Task ${task.id} has unfinished dependencies.`, 3);
  }

  return task;
}

function dependenciesDone(task: TaskRecord, tasks: TasksFile): boolean {
  return task.depends_on.every((depId) => {
    const dep = tasks.tasks.find((candidate) => candidate.id === depId);
    return dep?.status === "done";
  });
}

export function updateTaskStatus(
  tasks: TasksFile,
  taskId: string,
  status: TaskStatus,
  options?: { blockedReason?: string; prUrl?: string },
): TasksFile {
  const updated = tasks.tasks.map((task) => {
    if (task.id !== taskId) {
      return task;
    }

    const next: TaskRecord = {
      ...task,
      status,
    };

    if (status === "blocked") {
      next.blocked_reason = options?.blockedReason ?? "Blocked by governance or unresolved dependency.";
    } else {
      delete next.blocked_reason;
    }

    if (options?.prUrl) {
      next.pr_url = options.prUrl;
    }

    return next;
  });

  return {
    ...tasks,
    tasks: updated,
  };
}

export function ensureSingleInProgress(tasks: TasksFile, activeTaskId: string): void {
  const conflicting = tasks.tasks.filter((task) => task.status === "in_progress" && task.id !== activeTaskId);
  if (conflicting.length > 0) {
    const ids = conflicting.map((task) => task.id).join(", ");
    throw new HarnessError(`Only one task can be in_progress. Conflicting tasks: ${ids}`, 3);
  }
}
