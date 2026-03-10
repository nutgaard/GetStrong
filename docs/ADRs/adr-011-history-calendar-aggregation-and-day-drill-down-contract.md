# ADR-011: History calendar aggregation and day drill-down contract

Status: accepted

## Context
`T36` in `TASKS.yaml` defines the final screenshot-audit remediation bundle for History calendar behavior:

- calendar weeks must start on Monday
- a day with multiple completed workouts must keep a stable marker layout instead of changing cell structure
- tapping a day with one workout should open that workout directly
- tapping a day with multiple workouts should open a selection surface first

ADR-006 already fixes the top-level History boundary:

- `history` remains one top-level destination
- `List` and `Calendar` stay local sections inside `history`
- extra History sections should not become routes by default

The remaining decision is how to model calendar state and drill-down behavior without fragmenting History into extra routes or persisting presentation-specific calendar structures.

## Decision
Keep the History calendar as a pure projection over completed workout summaries inside the existing `history` destination. Use Monday-first date math, aggregate workouts by local completion date, render one stable marker per completed day, and handle day drill-down locally inside the History flow.

### State and data contract
The calendar is derived state, not stored state.

- The source of truth remains completed workout summaries/history records.
- The calendar view model derives a month grid and day aggregates from those summaries.
- Aggregate by local calendar date in the app/user time zone before rendering or handling taps.

Each calendar day cell is represented by one derived day model:

- `date`
- `isInDisplayedMonth`
- `workoutCount`
- `hasCompletedWorkout`
- `representativeMarkerState`
- `workoutRefs` for drill-down decisions

The marker model is intentionally stable.

- A completed day renders one consistent visual marker regardless of whether there is one workout or many.
- Marker density does not scale with workout count for this slice.
- Workout count may still exist in derived state for accessibility, semantics, or a later label/sheet, but it must not change the cell's basic structure.

Week layout is Monday-first.

- The calendar grid must be generated with Monday as the first day of week.
- Leading/trailing filler cells are derived from that Monday-first week model.
- Do not use a locale-default Sunday-first layout if it conflicts with the UX contract.

### Navigation and drill-down contract
No new top-level or child route is required for the History calendar remediation.

- `history` continues to own both `List` and `Calendar`.
- Day-tap behavior is resolved inside the History flow using the already-loaded day aggregate.

Day drill-down splits by cardinality:

- if `workoutCount == 0`: no drill-down
- if `workoutCount == 1`: open that workout directly in the existing completed-workout detail presentation already owned by History
- if `workoutCount > 1`: open a local selection surface first, then let the user choose which completed workout to open

The multiple-workout selection surface remains local UI owned by `history`.

- It may be a modal bottom sheet, dialog, or equivalent local surface
- it must not become a separate navigation route for this slice
- it receives the already-derived list of workouts for the selected date

### UI contract
The History screen keeps `List` and `Calendar` as sibling local sections.

- The calendar header and month paging remain local to the calendar section.
- Calendar cells are uniform and stable in size/structure across the month grid.
- A completed day uses one consistent completion marker.
- A day cell may expose richer semantics than its visuals, such as announcing the number of workouts on that date, but the visual marker remains singular and stable.

The direct-open versus choose-first behavior is part of the History section contract, not an implementation accident:

- single-workout days optimize for one-tap drill-down
- multi-workout days optimize for correct selection before opening detail

## Consequences
- The calendar remains a lightweight derived projection over existing history records rather than a second persisted history structure.
- Monday-first layout is guaranteed regardless of default locale week settings.
- The UI stays visually stable even when several workouts share a date.
- History avoids route sprawl because the multi-workout chooser stays local to the existing `history` destination.
