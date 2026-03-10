# ADR-008: Active workout inline session and resume contract

Status: accepted

## Context
`T39`, `T40`, and `T41` in `TASKS.yaml` define a bounded remediation bundle for the active-workout flow:

- warmup rows must be visible and actionable in the focused session screen
- the current actionable set must stay in workout order instead of being detached into a bottom-pinned dock
- the rest timer must remain a transient bottom overlay without causing layout collisions
- leaving the active-workout screen must not silently discard real progress

The current architecture already has the right high-level boundaries:

- `activeWorkout/{sessionId}` is a focused child route under ADR-006
- `SessionRepository` owns persisted session state
- `ActiveWorkoutViewModel` adapts repository state for UI
- `ActiveWorkoutScreen` renders local section state and set interactions

The missing piece is a tighter contract for how persisted session state, local section state, inline rendering, and resume behavior fit together.

## Decision
Keep `activeWorkout/{sessionId}` as a single focused route and keep `SessionRepository` as the source of truth for unfinished sessions and planned-set progress. Fix the remediation bundle by tightening the state, navigation, and layout contracts rather than introducing new route types or a separate active-session cache.

### State contract
Persisted session state continues to be the canonical record of session progress.

- `WorkoutSession` and `SessionPlannedSet` remain the durable source of truth for active-workout progress.
- The actionable set is still defined by domain order: `currentSet` is the first incomplete planned set by `setOrder`.
- Warmup and work sets remain in one persisted ordered plan. The UI may derive sections from `setType`, but it must not create a second ordering model that can diverge from `setOrder`.
- Exercise grouping in the UI is derived from the persisted plan:
  - groups are keyed by `workoutSlotId`
  - group order follows workout slot order as expressed by the ordered planned sets
  - set order inside each group follows persisted `setOrder`
- Extra sets remain part of the same persisted plan and must stay attached to the anchor exercise group in the section where they were created.

The active-workout UI may hold only lightweight presentation state locally:

- selected section (`Workout` or `Warmup`)
- highlighted set id
- transient dialog visibility
- transient rest-timer countdown state derived from a persisted timer target or recreated from saved state

The UI must not treat a bottom dock as a second source of truth for the current set. The current set is a row in the list first.

### Section-transition contract
`Workout` and `Warmup` remain local sections inside `activeWorkout`; they are not separate routes.

- The selected section defaults to the section containing `currentSet`.
- Users may manually inspect the non-current section, but the UI must automatically switch back when the current actionable phase changes.
- In particular, when the last warmup set for the current exercise is completed and the next actionable set is a work set, the screen returns focus to the `Workout` section for that same exercise.
- If a session has no warmup sets for any remaining exercises, the `Warmup` section may render as empty but must not replace the real session content with a placeholder when warmups do exist in the persisted plan.

### Layout contract
The active-workout screen is list-first.

- All exercise groups and set controls render inline in the scrollable session list.
- The current actionable set remains visible in its natural group position and is emphasized by styling, not by being relocated to a separate dock.
- A compact session overview strip may remain above the list for elapsed time, completion count, and section switching.
- The bottom of the screen is reserved for the rest-timer overlay only. It must not host a second interactive current-set surface.
- The list must use a stable bottom inset that accounts for a compact overlay, not a large dynamic padding strategy that causes the content to jump or float into itself.
- The rest overlay is informational and transient:
  - it is anchored to the bottom safe area
  - it may overlap only non-critical whitespace, not the currently actionable set row
  - it must not push headers, cards, or set controls into overlapping states

### Navigation and resume contract
The app supports one unfinished active workout at a time for this remediation slice.

- Starting a workout from Home or Programs must resolve to the existing unfinished session when one already exists, instead of creating a duplicate session.
- The reusable decision point belongs in domain/data entry logic, not in individual composables.
- `activeWorkout/{sessionId}` remains the only in-session route. Resume means navigating back to that persisted session id.
- Navigating away from `activeWorkout` does not end or discard a session that has any completed set.
- A newly started session with zero completed sets may be discarded only by an explicit exit path; it must not be lost implicitly by ordinary navigation or recomposition.
- The summary flow remains terminal: once a session is completed and handed off to `summary/{sessionId}`, that session is no longer resumable as an active session.

### Persistence boundary contract
Session progress persistence stays in Room-backed session data, not DataStore.

- `SessionRepository` owns discovery of unfinished sessions, planned-set progress, extra sets, and completion state.
- DataStore remains for lightweight defaults only; it must not become the store for active session content.
- Rest-timer countdown persistence remains lightweight and local to the active-workout presentation layer. It is acceptable to reconstruct countdown state from `SavedStateHandle` as long as the underlying session progress comes from `SessionRepository`.

## Consequences
- Warmups, work sets, and extra sets stay inside one durable session model, reducing drift between domain state and UI state.
- Removing the bottom-pinned current-set dock simplifies the active-workout layout and aligns the presentation with the reference screenshots.
- Resume behavior becomes a repository/use-case concern instead of a fragile navigation-side convention.
- The remediation stays bounded: no new route family, no new persistence technology, and no redesign of summary/history behavior.
