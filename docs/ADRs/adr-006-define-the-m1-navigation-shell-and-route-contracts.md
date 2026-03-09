# ADR-006: Define the M1 navigation shell and route contracts

Status: accepted

## Context
ADR-002 chose single-activity Compose navigation, but milestone 1 still needs a concrete shell that matches the product flow. The original flow-oriented route list was enough to unblock early implementation, but the screenshot pack in `docs/images/` now defines a clearer shell:

- a persistent bottom navigation bar for the primary browse areas
- nested tab/section switching inside some destinations
- focused edit and in-workout screens that intentionally drop the bottom navigation chrome

Without documenting those contracts, product and UI documentation will drift toward the wrong screen boundaries.

## Decision
Use a single `NavHost` in `MainActivity` as the app shell for MVP. The navigation model is split between top-level shell destinations and focused child flows.

Top-level shell destinations keep the persistent bottom navigation visible:

- `home`: launch point showing upcoming workouts and the primary "start workout" action
- `programs`: workout/program management entry point
- `history`: completed workout history entry point
- `progress`: progression overview entry point
- `settings`: app settings and training defaults

Focused child flows are separate destinations and do not share the persistent bottom navigation chrome:

- `workoutEditor/{workoutId?}`: create/edit a workout and reorder its exercises
- `exerciseDetail/{exerciseId}`: exercise configuration and drill-down views
- `activeWorkout/{sessionId}`: in-session workout execution
- `summary/{sessionId}`: completed workout summary

Screen-internal tab rows shown in the screenshot pack are presentation structure first, not automatically separate app routes. The current contract is:

- `programs`, `history`, `exerciseDetail`, and `activeWorkout` may each host local tab/section state inside the destination
- promote a tab/section to its own child route only when it needs stable deep-linking, independent back-stack behavior, or materially different route arguments

Navigation remains part of `ui/navigation`. Route arguments must be stable identifiers such as `workoutId`, `exerciseId`, and `sessionId`. Do not pass Room entities, domain aggregates, or mutable state through routes.

The `activeWorkout` flow remains intentionally full-screen. It may expose local `Workout` and `Warmup` sections, but it should not share persistent navigation chrome with the primary browse destinations.

## Consequences
- The documented shell now matches the screenshot pack's primary information architecture instead of a generic planning/execution split.
- The route graph stays small because screen-local tabs do not become routes by default.
- Edit screens and in-workout flows have explicit room to be immersive and task-focused without bottom navigation.
- Route contracts remain durable because they carry identifiers rather than object graphs.
- If future product requirements need direct links into a specific tab or subview, that subview can be promoted to a child route without revisiting ADR-002.
