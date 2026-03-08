# ADR-006: Define the M1 navigation shell and route contracts

Status: accepted

## Context
ADR-002 chose single-activity Compose navigation, but milestone 1 still needs a concrete shell that matches the product flow. The starter tab scaffold does not reflect the real workout experience and would bias the implementation toward the wrong UI structure.

## Decision
Use a single `NavHost` in `MainActivity` as the app shell for MVP. The initial navigation model is flow-oriented:

- `home`: launch point and top-level entry to workouts
- `planning`: workout list and workout editor flow
- `activeWorkout`: focused in-session workout flow
- `summary`: completed workout summary flow

Navigation remains part of `ui/navigation`. Route arguments must be stable identifiers such as `workoutId` and `sessionId`. Do not pass Room entities, domain aggregates, or mutable state through routes.

The `activeWorkout` flow is intentionally full-screen and should not share persistent navigation chrome with planning screens. This refines ADR-002 without changing the single-activity decision.

## Consequences
- `M1` gets a navigation shell that matches both planning and execution use cases.
- Route contracts remain durable because they carry identifiers rather than object graphs.
- The placeholder tab scaffold should be replaced, not evolved into the production shell.
- Future destinations can be added as long as they follow the same routing contract pattern.
