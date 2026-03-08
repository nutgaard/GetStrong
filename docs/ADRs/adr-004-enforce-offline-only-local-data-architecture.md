# ADR-004: Enforce offline-only local data architecture

Status: accepted

## Context
The PRD requires the exercise catalog, workouts, workout summaries, and progression behavior to work fully offline. Authentication, cloud sync, and remote APIs are explicitly out of scope for MVP.

## Decision
Treat all MVP repositories as local-only and offline-first.

- Do not add network dependencies, sync engines, or remote repository abstractions during MVP.
- Persist structured product records locally.
- Persist lightweight application settings locally.
- Keep repository contracts focused on product capabilities, not on potential future remote sources.

This ADR sets the product boundary: all data access is local. Concrete storage technology choices inside that local-only boundary may be refined by later ADRs without changing the offline-only decision itself.

## Consequences
- The architecture stays aligned with MVP scope and avoids speculative remote complexity.
- Workouts, exercise catalog data, summaries, and progression history can be modeled without cloud assumptions.
- Local schema evolution and migrations become a normal engineering concern early.
- Any future introduction of sync, accounts, or network-backed repositories requires a new ADR because it changes a core product assumption.
