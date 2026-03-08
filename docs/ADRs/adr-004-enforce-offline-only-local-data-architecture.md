# ADR-004: Enforce offline-only local data architecture

Status: accepted

## Context
The PRD explicitly requires an offline local exercise catalog, local persistence of workouts and summaries, and excludes authentication or cloud features. The base architecture should reflect those product constraints immediately.

## Decision
Design repositories as local-first and offline-only. Do not add network dependencies or remote data abstractions. When structured persistence is implemented, use a local database approach suitable for Android offline storage, with Room as the default choice for persisted entities and seeded local catalog data.

## Consequences
- The architecture stays aligned with MVP scope and avoids unnecessary remote complexity.
- Persistence decisions can support workouts, exercise catalog, summaries, and progression history without redesigning repository contracts.
- Database schema evolution and migrations become an early technical concern.
- Any later introduction of sync or cloud capabilities will require a new ADR because it changes core assumptions.
