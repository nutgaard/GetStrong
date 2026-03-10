# ADR-007: Use Room for structured records and DataStore for local settings

Status: accepted

## Context
ADR-004 establishes that MVP data is entirely local, but milestone 1 still needs a concrete storage split. The app has two distinct persistence shapes:

- relational product records such as workouts, exercises, sessions, and summaries
- lightweight user defaults such as rest duration and load increments

Using a single storage mechanism for both would either overcomplicate settings or under-model relational workout data.

## Decision
Use a split local persistence model inside the offline-only boundary defined by ADR-004:

- `Room` is the system of record for structured product data.
- `DataStore` is the system of record for lightweight application settings.

`Room` stores:

- exercise catalog records
- workouts and ordered workout exercise slots
- active workout sessions and set results
- completed workout summaries and progression history

`DataStore` stores:

- default rest duration
- default dumbbell or machine increment
- default deload percent
- recurring weekly training-day selection for Home/Programs schedule planning
- other app-wide defaults that are preferences rather than business records

Repository interfaces remain in `domain`, while storage technology details stay in `data`.

## Consequences
- `M1` gains a concrete persistence plan without violating ADR-004.
- Structured workout data can be modeled relationally from the start, reducing rework later.
- Preference changes remain lightweight and do not require schema migrations.
- Room migrations become a normal maintenance concern from the first persisted release.
