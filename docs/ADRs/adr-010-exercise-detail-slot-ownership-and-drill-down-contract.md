# ADR-010: Exercise detail slot ownership and drill-down contract

Status: accepted

## Context
`T38` in `TASKS.yaml` defines the current remediation bundle for exercise detail:

- from workout editing, the user must be able to open an exercise-detail surface
- that surface must expose sets x reps, working weight, progression mode, increment, deload, and plate guidance
- that surface must also provide navigation into per-exercise Progress and History

The current model already splits concerns in a useful way:

- `WorkoutExerciseSlot` owns workout-specific prescription and progression state
- `Exercise` owns exercise identity and metadata such as equipment type
- `ExerciseProgress` and `ExerciseHistory` are already routed and keyed by `exerciseId`
- `WorkoutEditor` currently owns the selected workout draft

What is missing is a concrete contract for the exercise-detail route, draft ownership inside the workout-editing flow, and the boundary between slot-scoped configuration and exercise-level drill-downs.

## Decision
Promote exercise detail to a focused child route inside the workout-editing flow. Keep workout-slot configuration as slot-owned draft state within the workout-editor flow, while Progress and History drill-downs stay exercise-owned and keyed by `exerciseId`.

### State and data ownership contract
Exercise detail is a slot-detail screen, not a global exercise-settings screen.

- The source of truth for the editable fields shown in exercise detail is the selected `WorkoutExerciseSlot` draft:
  - target sets / target reps
  - rep range
  - current working weight
  - progression mode
  - increment
  - deload percent
  - rest override if surfaced later in the same flow
- These values remain workout-specific. They must not be written back to the global `Exercise` entity.
- `Exercise` remains read-only reference metadata for this screen:
  - exercise id
  - exercise name
  - equipment type
  - metadata needed to derive plate guidance

Plate guidance is a derived presentation field.

- Plate guidance is computed from the slot's current working weight plus the exercise/equipment metadata.
- Plate guidance is not a separately persisted field.

The workout-editor flow remains the owner of the mutable draft.

- Exercise detail edits mutate the same workout draft that `workoutEditor` will eventually save.
- Opening exercise detail must not require persisting the workout first.
- Saving the workout remains the responsibility of the workout-editor flow, not the detail screen in isolation.

### Navigation contract
Add a focused child route for exercise detail under the Programs/workout-editing flow.

- `workoutEditor/{workoutId}` remains the parent editing flow.
- Add an `exerciseDetail/{workoutId}/{exerciseId}` child route, or the equivalent nested route form under `workoutEditor`.
- The route key is the workout context plus `exerciseId`.

This route contract is acceptable because duplicate exercises inside one workout are already prohibited, so `exerciseId` uniquely identifies the slot inside the current workout draft.

No new top-level destination is introduced.

- Exercise detail remains a focused child flow without bottom-navigation chrome.
- Back/up from exercise detail returns to `workoutEditor` with the draft intact.

Progress and History drill-downs remain exercise-owned child routes.

- Navigating from exercise detail to Progress uses `exerciseProgress/{exerciseId}`.
- Navigating from exercise detail to History uses `exerciseHistory/{exerciseId}`.
- These drill-downs do not own or mutate the workout-slot draft.

### UI contract
Exercise detail is the detailed editor for one slot in one workout.

- The top of the screen identifies the selected exercise and the current working weight context.
- The primary editable content is slot detail:
  - sets x reps / rep range
  - working weight
  - progression mode
  - increment
  - deload
  - plate guidance
- Progress and History are navigational drill-down actions or local sections that lead to the existing `exerciseProgress` and `exerciseHistory` routes.
- The screenshot-only `Form` tab remains out of scope and hidden until separately defined.

The screen must not split ownership incorrectly:

- editing slot settings happens in exercise detail
- workout-level concerns such as workout name and slot ordering remain in workout editor
- exercise-level historical analytics remain in Progress/History drill-down routes

## Consequences
- The user gets a focused detail editor without overloading the main workout-editor screen.
- Slot-specific configuration remains correctly scoped to one workout instead of leaking into global exercise metadata.
- Progress and History reuse the existing exercise drill-down routes and stay analytics-oriented rather than becoming editors.
- The draft can stay unsaved while the user moves between workout editor and exercise detail, reducing forced-save friction.
