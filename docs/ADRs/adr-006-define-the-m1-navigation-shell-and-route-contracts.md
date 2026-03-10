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

The top-level shell is production-only. Dev/demo/debug actions such as persistence demos, catalog loaders, or temporary implementation shortcuts must not appear as shell destinations or as persistent actions on `home` and other top-level screens.

Focused child flows are separate destinations and do not share the persistent bottom navigation chrome:

- `workoutEditor/{workoutId?}`: create/edit a workout and reorder its exercises
- `activeWorkout/{sessionId}`: in-session workout execution
- `summary/{sessionId}`: terminal post-session summary and dismissal flow

Screen-internal tab rows shown in the screenshot pack are presentation structure first, not automatically separate app routes. The current contract is:

- `programs`, `history`, and `activeWorkout` may each host local tab/section state inside the destination
- promote a tab/section to its own child route only when it needs stable deep-linking, independent back-stack behavior, or materially different route arguments

For the current Programs/workout CRUD scope, only the workout-management path is committed:

- `programs` exposes the workout overview/list behavior
- `workoutEditor` owns the selected-workout draft, its ordered slot list, the add-exercise picker entry point, and the minimum slot-scoped editing needed for this flow
- slot-specific editing may remain local inside `workoutEditor` for the minimum shippable T6 implementation; promote it to a child route later only if deeper slot editing or navigation needs justify it
- unresolved secondary Programs tabs are explicitly deferred and must not block the workout CRUD implementation

For the current active-workout interaction scope, only the session-execution path is committed:

- `activeWorkout` remains a single focused child flow for one session and may host local `Workout` and `Warmup` sections inside that route rather than splitting them into separate destinations
- the set-circle interaction contract applies in both sections: tapping an incomplete set marks it complete at target reps, and tapping it again decrements achieved reps by one until it reaches zero
- completed or partially completed sets stay visible in place; the interaction updates set state rather than removing the row or moving the user to another screen
- extra sets are added inline from the trailing `+` affordance in the current session view rather than through a separate creation route
- per-set secondary actions such as setting reps, setting weight, clearing/resetting a set, or removing an extra set are contextual UI attached to the active-workout screen rather than separate top-level navigation
- rest timing feedback stays as a transient bottom overlay within `activeWorkout`; it must not replace the session screen with a blocking timer destination or modal flow

For the current post-workout summary scope, only the terminal review path is committed:

- `summary` is entered from `activeWorkout` after a session is completed and is not exposed as a top-level destination
- `summary` remains a focused child flow without bottom-navigation chrome
- `summary` surfaces total time, total volume, and per-set results with warmup versus work-set distinction
- the primary dismiss action exits to the top-level app shell rather than returning the user to a completed active-workout session

Navigation remains part of `ui/navigation`. Route arguments must be stable identifiers such as `workoutId` and `sessionId`. Do not pass Room entities, domain aggregates, or mutable state through routes.

Back/up behavior follows standard Android rules:

- top-level shell destinations are switched by bottom navigation, not by in-content back buttons
- moving between top-level destinations should preserve normal shell behavior rather than building ad-hoc screen-to-screen back chains
- focused child flows provide top app bar back/up navigation that pops back to the previously visible shell destination or parent screen, except for terminal flows such as `summary` whose primary dismissal returns directly to the top-level shell

The `activeWorkout` flow remains intentionally full-screen. It may expose local `Workout` and `Warmup` sections, but it should not share persistent navigation chrome with the primary browse destinations.

## Consequences
- The documented shell now matches the screenshot pack's primary information architecture instead of a generic planning/execution split.
- The route graph stays small because screen-local tabs do not become routes by default.
- Edit screens and in-workout flows have explicit room to be immersive and task-focused without bottom navigation.
- Route contracts remain durable because they carry identifiers rather than object graphs.
- If future product requirements need direct links into a specific tab or subview, that subview can be promoted to a child route without revisiting ADR-002.
