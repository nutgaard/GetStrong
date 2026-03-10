# ADR-009: Home schedule and Programs action contract

Status: accepted

## Context
`T35` and `T37` in `TASKS.yaml` define the `M12-B2` remediation bundle:

- Home must show a schedule-driven upcoming queue instead of a static next-up card
- schedule configuration must live in the Programs flow
- Home cards and the Home FAB must launch workouts consistently
- Programs rows must use overflow menus instead of the current inline `Workout Actions`
- planning screens must share one floating-action-button inset treatment

Existing ADRs already fix the large boundaries:

- ADR-006 keeps `home` and `programs` as top-level shell destinations and prefers local section state over new routes
- ADR-007 keeps lightweight app-wide configuration in DataStore and structured workout/session records in Room
- ADR-008 already establishes that starting a workout must resolve to an unfinished active session instead of creating duplicates

This remediation bundle needs a tighter contract for where schedule data lives, how Home derives its queue, and how Programs rows and FAB chrome are structured.

## Decision
Keep schedule configuration in the `programs` destination as local Programs UI, but persist the weekly training-day selection in the existing app-settings/DataStore boundary. Derive the Home queue from that persisted weekly schedule plus saved workout order and current session/history state. Standardize Programs row actions and planning FAB spacing as shared UI patterns, not screen-specific one-offs.

### State and data contract
The weekly schedule for this slice is a lightweight recurring app-level configuration, not a new relational planning entity.

- Persist the schedule as a normalized set/list of training weekdays inside `AppSettings`.
- The persisted representation must be stable and Monday-first.
- Scope for this slice is weekday selection only:
  - which weekdays are training days
  - no per-day workout assignment
  - no calendar exceptions
  - no multi-week template engine

This keeps the storage shape inside the ADR-007 DataStore boundary while the UI entry point lives in Programs.

The Home queue is a derived projection, not stored state.

- Build the queue from:
  - saved workouts in deterministic workout order
  - persisted training weekdays
  - current date
  - last completed workout when no active session exists
  - unfinished active session when one exists
- The queue covers the next 7 calendar days and emits only scheduled workout occurrences inside that window; non-training days do not need placeholder cards.
- Queue generation must be deterministic across app restarts.

Workout ordering for Home rotation must follow the workout-planning order, not ad hoc card order. If the repository already exposes explicit position, use it. If not, use the current stable workout ordering contract until a later planning task changes it.

### Navigation and launch contract
No new routes are introduced for this bundle.

- `programs` remains the configuration entry point for workout planning and weekly schedule editing.
- Schedule configuration is local UI inside `programs`; it is not promoted to a child route unless later requirements need deep links or separate back-stack behavior.
- `home` remains the queue display and quick-start entry point.

All workout launches from Home must go through one shared start-or-resume decision path.

- Tapping a Home card and pressing the Home FAB must call the same domain/use-case entry point.
- That entry point returns a launch target for `activeWorkout/{sessionId}`.
- If an unfinished session exists, launch resolves to that existing session instead of creating a duplicate.
- If no unfinished session exists, launch starts the selected workout and returns the created session id.
- The primary Home FAB always launches the first actionable item shown in the Home queue.

The Home UI may reflect resume state in copy or badge treatment, but launch resolution must not be implemented independently in the composable layer.

### Programs UI contract
Schedule configuration lives in Programs as part of the same planning surface as workouts.

- The shipped Programs route keeps one destination and may host local sections within the same screen.
- The weekly schedule editor belongs to the Programs surface, not Settings, for this slice.
- The schedule editor may render inline on the Programs screen or through a local modal surface owned by Programs, but it must stay within the `programs` route boundary.

Workout and exercise rows use a row-primary / menu-secondary interaction model.

- Row tap remains the primary action:
  - workout row tap opens the workout editor
  - exercise row tap continues to open or focus its owning editor action
- A trailing three-dot overflow icon owns contextual actions such as delete.
- Overflow menu open/close state is local UI state only and must not leak into domain models.
- Contextual actions dispatch intent-style events upward to the owning view model; the row menu itself does not perform repository work directly.

### Shared FAB chrome contract
Planning FAB spacing is a shared presentation primitive.

- Programs and workout-editor screens must use one shared inset/padding contract instead of hardcoded per-screen values.
- The shared contract must account for safe drawing/navigation bars and any scaffold chrome so the visible FAB position is consistent between top-level Programs and focused planning child screens.
- The shared FAB treatment is a UI token/helper/component concern only; it must not introduce new domain state or route state.

## Consequences
- The schedule remains lightweight to persist and easy to observe, while still being edited from the user-expected Programs area.
- Home queue generation becomes a pure derived-use-case problem instead of hand-built screen logic.
- Home card launches and FAB launches cannot drift because they share one start/resume contract.
- Programs interaction patterns align with the reference screenshots without creating extra routes or menu-specific domain state.
- If future scope adds assigned workouts per weekday or richer calendar planning, revisit this ADR and consider promoting schedule storage from DataStore into Room-backed planning records.
