# AI Harness

Autonomous multi-agent harness for this repository, implemented with OpenAI Agents SDK.

## Agents

- `planner`: creates executable plan and blocks only when truly missing required info.
- `architect`: sets high-level principles, boundaries, and dependency rules.
- `implementer`: edits repo, runs commands, and commits directly while adhering to architect constraints.
- `verifier`: analyzes validation outputs and provides repair hints.

## Governance Docs

- `PRD.md`: user-maintained product intent.
- `TASKS.yml`: planner-maintained task backlog/priority/status/dependencies.
- `ARCHITECTURE.md`: architect-maintained high-level technical guidance.
- `docs/ADRs/*.md`: architect-maintained major architectural decisions.

## Usage

From repo root:

```bash
bun run ai -- sync-plan
bun run ai -- execute-next
bun run ai -- autopilot
bun run ai -- reconcile
```

Optional flags:

- `--task <task-id>` (force a specific ready task in `execute-next`)
- `--max-iterations <n>`
- `--repair-attempts <n>`
- `--base-branch <name>`
- `--no-pr`
- `--draft-pr`

## Live Progress

- Agent runs now stream live model milestones to the terminal.
- You will see events for model response start/done, reasoning item creation, tool calls/outputs, and agent completion.
- Token-by-token text deltas are intentionally suppressed in default output to avoid terminal spam.

## Required Environment

- `OPENAI_API_KEY` must be set.
- Optional: `OPENAI_MODEL` (default `gpt-4.1-mini`).

Use `.env.example` as template.

## Run Artifacts

Each run stores artifacts in `.ai-runs/<run-id>/`:

- `events.jsonl` (full event stream, including model milestones)
- `request.json`
- `preflight.json`
- `planner-output.json`
- `architect-output.json`
- `implementer-output-*.json`
- `verifier-output-*.json`
- `governance-audit-attempt-*.json`
- `verification.log`
- `pr-body.md`
- `state.json`
- `artifact-index.json`
- `summary.txt`

## Exit Codes

- `0`: success and checks passed
- `2`: PR opened but checks still failing after repair attempts
- `3`: planner blocked waiting for clarification
- `4`: preflight/policy/runtime failure
