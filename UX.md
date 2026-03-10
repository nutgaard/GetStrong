# UX Document

This file is an AI-authored synthesis of the screenshots in `docs/images` plus the user-authored notes in `docs/images/image-description.md`.

## Overall Structure

- The top-level app shell uses a bottom navigation bar with five destinations: `Home`, `Programs`, `History`, `Progress`, and `Settings`.
- Top-level destinations use consistent production screen chrome: a stable title/top-bar treatment paired with the shared bottom navigation shell, without dev/demo utility actions mixed into the screen content.
- Where a top-level destination has sub-areas, they stay as local tabs/sections instead of becoming separate top-level destinations.
- The active workout flow is a focused full-screen mode and does not show the bottom navigation bar.
- Completing an active workout leads into a focused post-workout summary flow before returning to the top-level shell.

## Top-Level Navigation

- `start_screen.png` represents `Home`.
- `workout_overview.png` represents the `Programs` area with the `Workouts` tab selected.
- `history_workout.png` and `history_calendar.png` represent the `History` area with `List` and `Calendar` selected.
- `progress.png` represents the `Progress` area.
- `Settings` is visible in the bottom navigation bar but no dedicated screenshot was provided for its screen.

## Screen Summaries

- `start_screen.png`: Home shows an upcoming workout queue as cards. Each card includes the workout name, scheduled date, a few upcoming lifts, and a collapsed count of additional exercises. A prominent `Start Workout` FAB launches the next planned session.
- For the current bounded Home slice, the important contract is the structural start-screen pattern: upcoming workout cards plus a primary quick-start action. The exact long-term scheduling algorithm behind the queue is not yet part of the committed UX contract.
- `workout_overview.png`: The reference pack shows the `Programs` area with `Workouts` active. On the current branch, the committed surface is the saved-workout list plus row-level start/edit/delete actions and a red add-workout FAB. `Program`, `Weights`, and `Sets×Reps` remain explicitly deferred and hidden until a later task defines their scope.
- `edit_workout.png`: The workout editor shows the ordered exercise list for a single workout. On the current branch, reordering and removal stay local to the editor via per-row actions, minimum sets/reps editing also stays local to the editor, and the FAB opens a picker of exercises not already in the workout.
- `edit_exercise.png`: The reference pack includes a richer exercise-detail screen tabbed into `Weight`, `Form`, `Progress`, and `History`. The provided screenshot shows `Weight`, with sets x reps, current/next working weight, progression settings, deload settings, and a plate helper. On the current branch's bounded T6 path, this deeper drill-down is not required yet; minimum slot editing remains local inside the workout editor.
- `progress.png`: The progress overview is the top-level `Progress` surface. It lists exercises with the latest working weight and a compact sparkline-like trend, and tapping an item opens that exercise's dedicated progress drill-down.
- `progress_exercise.png`: The per-exercise progress screen is a focused drill-down view. It shows a larger line chart for a selected exercise and a recent time-range selector (`3M` in the screenshot).
- `Settings` (no dedicated screenshot): Settings is a top-level training-defaults form. It exposes rest duration, load increment, deload percent, and default progression mode; those values prefill new sessions and new workout slots but do not rewrite existing saved slot configuration.
- `history_workout.png`: The history list shows completed workouts as cards, with date, per-exercise performed results, and total duration.
- `history_calendar.png`: The calendar view marks workout-completion dates with red circles. It sits under the same `History` shell as the list view, as a local section rather than a separate top-level route.
- `history_exercise.png`: The per-exercise history table lists individual non-warmup sets with date, reps, weight, and estimated 1RM. It is a focused drill-down view rather than another local `History` tab.
- `workout_in_progress_0.png`: The active workout screen shows the `Workout` tab before significant completion. Each exercise row has circles for planned sets, a target prescription on the right, and access to additional set actions.
- `workout_in_progress_1.png`: The same screen shows partial progress, including support for partial reps and a bottom rest timer overlay.
- `workout_in_progress_2.png`: The same screen later in the session shows more completed sets and a rest overlay with the configured rest duration.
- `workout_warmup.png`: The `Warmup` tab lists warmup sets for the current exercise with weight prescriptions and per-side loading guidance. It is the warmup companion to the `Workout` tab inside the same active session, and the section focus returns to `Workout` when the current exercise's warmups are complete.
- `summary` (no dedicated screenshot): The post-workout summary is a focused terminal screen that shows session totals and per-set results with warmup/work distinction, then returns the user to the main shell through a primary dismiss action.

## Repeated Interaction Patterns

- Red is the primary action/completion color across the reference pack.
- Top-level areas use a bottom navigation bar; nested content uses tabs near the top of the screen.
- Top-level shell screens should feel like siblings in the same app shell rather than one-off layouts; title treatment, spacing rhythm, and entry-point chrome should stay consistent across `Home`, `Programs`, `History`, `Progress`, and `Settings`.
- Ordering affordances stay attached to the list row. The reference pack shows drag handles, while the current bounded workout-editor flow uses explicit move actions from the row menu.
- Row-specific destructive or contextual actions use a right-side menu affordance. The reference pack shows three-dot overflow menus; the current branch uses labeled row menus in the bounded Programs/editor flows.
- Creation actions use a red floating action button.
- Active workout completion uses large circular set controls instead of small inline buttons.
- The active workout flow supports partial reps by repeated taps on the same set control.
- Extra sets can be added inline with a trailing `+` circle.
- Rest timing feedback appears as a transient bottom overlay rather than a full-screen interruption.
- Active workout hierarchy prioritizes the current set and set-circle interaction area; timers, counts, tabs, and helper copy stay visually secondary.
- The reference pack does not surface history, charts, or settings content during active workouts; the session screen stays focused on execution and short supporting status only.
- One-handed use favors keeping the current set, the next actionable set circles, and the extra-set affordance within easy thumb reach without requiring precise top-of-screen targets during normal workout progression.
- Secondary actions may still use long-press or contextual surfaces, but primary workout progression should not depend on repeated travel to distant header controls.
- Empty and error states stay inside the existing screen shell instead of replacing it with standalone fallback pages; top bars, tabs, bottom navigation, and primary screen headings remain intact where they are part of the normal flow.
- Empty states use contextual messaging with a clear next action, while recoverable error states use user-readable copy plus a direct retry or recovery action.

## Accessibility And TalkBack

- Accessibility work for the current bounded pass is limited to `Home`, `Programs`, `Active Workout`, and `Summary`.
- Screen structure should remain understandable through spoken navigation alone: top app bar first, then the primary screen heading or status, then the main content and primary action.
- Important meaning must not rely on color alone; `warmup` versus `work`, completion state, and destructive/contextual actions need readable text or spoken labels in addition to visual styling.
- Primary buttons, floating actions, overflow/context actions, and retry/recovery actions need explicit accessible names that describe their result rather than generic labels alone.
- Repeated cards or rows should expose a useful spoken summary before the user activates them, so TalkBack users can identify the workout or result without opening each item.
- For active workout specifically, the current set, section switcher, set-circle controls, extra-set affordance, contextual set actions, and finish action must all be reachable in a stable order without trapping focus in the rest overlay or transient dialogs.
- Set controls in active workout should announce enough state to act safely without sight: exercise, warmup/work section, set order, target reps, achieved reps if any, and whether the set is complete.
- Summary rows should announce warmup/work distinction, exercise name, target reps, achieved reps, and recorded load so the post-workout review is not dependent on card color or visual grouping.

## Navigation Relationships

- `start_screen.png` leads into the active workout flow via `Start Workout`.
- On the current branch, selecting a workout row in `workout_overview.png` opens `edit_workout.png`, while starting a specifically chosen saved workout remains a row-level action from the same Programs surface.
- The reference pack suggests `edit_workout.png` can lead to `edit_exercise.png` when an exercise is selected, but the current bounded T6 implementation keeps minimum exercise-slot editing local inside the workout editor instead of requiring a dedicated drill-down.
- `edit_exercise.png` exposes the `Progress` and `History` views for that exercise, represented by `progress_exercise.png` and `history_exercise.png`.
- `progress.png` leads to `progress_exercise.png` as a focused per-exercise drill-down rather than another top-level or local Progress section.
- `history_workout.png` and `history_calendar.png` are alternate views under the same `History` area.
- `history_workout.png` can drill down into `history_exercise.png` for a selected exercise without promoting that screen to another top-level History section.
- `workout_in_progress_0.png`, `workout_in_progress_1.png`, `workout_in_progress_2.png`, and `workout_warmup.png` are all views of the same active workout session.
- The active session keeps `Workout` and `Warmup` as local sections inside `activeWorkout`, and the visible section follows the current set phase as the session advances.
- Completing the active workout leads to a focused summary screen, which then dismisses back to the main app shell. No dedicated summary screenshot is currently included in the reference pack.

## Assumptions And Ambiguities

- The reference pack includes a dedicated `edit_exercise.png` screen, but the current bounded T6 implementation only commits local slot editing inside `edit_workout.png`; a richer exercise-detail drill-down is deferred.
- The exercise detail screen shows a `Form` tab, but no screenshot or prose describes its contents.
- The `History` area shows a `Notes` tab in the reference pack, but its behavior is not defined and it is deferred/out of scope for the current bounded `T30` implementation.
- The Home screen clearly implies scheduled upcoming workouts, but the exact scheduling rules behind the `A/B` alternation are still undefined; the current bounded Home implementation may use a simple deterministic ordering heuristic while preserving the start-screen structure.
- No dedicated summary mock is currently provided, so the post-workout summary content is driven by the task contract: per-set results, warmup/work distinction, total time, and total volume.
- The reference pack does not show any additional top-level or nested-tab Progress states beyond the overview list and per-exercise chart drill-down.
- The reference pack only shows populated states for `Home`, `Programs`, and `History`, so empty/error-state behavior is inferred from the task contract and should preserve those same shells rather than introducing separate fallback layouts.
