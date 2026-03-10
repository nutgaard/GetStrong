# Product Requirements Document

## Goal
Build an Android app that helps weightlifters get stronger and replaces the need for a paid StrongLifts 5x5 subscription.

The app should support custom programming, clear workout execution, and consistent progression tracking.

## Users
Primary users are people doing resistance training who want full control over workout structure and progression logic.

Users need to:
- Create and edit their own workouts.
- Track set-by-set performance.
- Apply progressive overload rules per exercise.
- Get guided through workouts with minimal interaction cost.

## MVP Scope
- Home and navigation:
- Show a Home screen with upcoming saved workouts as cards, including the next few queued sessions.
- Provide a prominent `Start Workout` FAB on Home to launch the next planned workout quickly.
- For the current bounded branch, the Home queue may use a simple deterministic ordering heuristic from saved workouts and recent completion history rather than a full scheduling engine.
- Use a five-item bottom navigation shell for the top-level areas: `Home`, `Programs`, `History`, `Progress`, and `Settings`.

- Workout planning:
- Create, edit, delete multiple named workouts (for example Push/Pull/Legs).
- Expose workout planning from the `Programs` area, with only the `Workouts` surface committed on the current branch.
- Keep the screenshot-only `Program`, `Weights`, and `Sets×Reps` tabs hidden on the current branch until their scope is explicitly defined.
- Add ordered exercises to each workout and manage them from the workout editor.
- Use a picker of remaining exercises rather than listing the entire catalog inline inside the workout editor.
- Persist workouts between sessions.

- Exercise catalog:
- Ship with a built-in catalog of **50 common exercises**.
- Each exercise includes primary/secondary muscle groups and equipment type.
- Catalog is local/offline.

- Exercise detail:
- The screenshot pack includes a richer `edit_exercise` reference with `Weight`, `Form`, `Progress`, and `History` tabs.
- The current branch does not require a dedicated exercise-detail drill-down to ship the bounded workout-planning flow; minimum slot editing remains local inside the workout editor.

- Workout execution:
- A user can start a saved workout from Home quick-start or from the Programs surface for a specifically chosen workout.
- Start a planned workout and be guided in this order:
- Warmup sets for Exercise A
- Work sets for Exercise A
- Warmup sets for Exercise B
- Work sets for Exercise B
- Continue until workout is complete.
- The active workout flow keeps `Workout` and `Warmup` as local in-session sections inside the same focused route.
- User may mark **any set** (warmup or work set) as completed at any time via one tap.
- User may add extra sets inline from the workout screen.
- Keep screen awake during active workout.

- Settings:
- Expose a top-level Settings screen for training defaults: rest duration, load increment, deload percent, and default progression mode.
- These defaults prefill new sessions and new workout slots only; existing saved workout slots keep their stored values until edited directly.

- Progression models:
- Per exercise, support:
- `weight_only`: increase weight when all target reps are achieved.
- `reps_only`: increase reps within configured rep range.
- `reps_then_weight`: increase reps to rep-range max, then increase weight and reset reps to rep-range min.
- Each **exercise slot in a workout** stores its own progression configuration (mode, rep range, increment, deload percent, and working-set prescription).
- When a user adds a new exercise slot to a workout, the app applies sensible per-slot defaults:
  - Default progression mode: `weight_only`.
  - Default increment: `+2.5 kg`.
  - Default working-set prescription:
    - Most exercises: 5 sets x 5 reps.
    - Deadlift: 1 set x 5 reps.
  - Default deload percent: `10%`.
- Global settings may change what new slots default to, but **actual training behavior always reads the stored configuration on each exercise slot**, not a global progression setting.
- Failure rule:
- A workout for an exercise is considered failed if **any work set** is below target reps.
- Deload rule:
- After 3 consecutive failed workouts for an exercise, reduce working weight by configurable deload percent.
- Default deload percent: `10%`.

- Timers:
- Workout elapsed timer from workout start to finish.
- Rest timer between work sets.
- Default rest duration: `3 minutes`.
- Sound notification when rest timer ends.
- No rest timer between warmup sets.
- Rest timer starts after the last warmup set and before the first work set.

- Warmup generation (deterministic, no online lookup):
- Warmups are generated from planned working weight.
- Warmup loads are rounded to achievable gym loads.
- Default rounding:
- Barbell loads to nearest `2.5 kg` total.
- Dumbbell/machine loads to nearest available increment (default `1.0 kg`).
- Heuristic:
- If working weight `< 20 kg`: one warmup set at `~55%` of working weight (minimum `5 kg`, rounded).
- If `20–79.9 kg`: one warmup set at `~60%` (rounded).
- If `80–119.9 kg`: warmup sets at `~45%, ~65%, ~80%` (rounded).
- If `>= 120 kg`: warmup sets at `~43%, ~57%, ~71%, ~86%, ~93%` (rounded).
- Example target behavior:
- `9 kg` lateral raise => include `5 kg` warmup.
- `140 kg` deadlift => include `60, 80, 100, 120, 130 kg` warmups.

- Workout summary:
- At workout completion, show:
- Reps achieved per set.
- Total lifted volume.
- Total workout time.
- Dismissing the summary returns to the top-level shell rather than back into the completed workout session.
- Persist workout summary for later analytics/charts (charts themselves are out of MVP).

- History and progress views:
- Provide a history area with at least `List` and `Calendar` views of completed workouts.
- Provide per-exercise history drill-downs for non-warmup sets.
- Provide a progress overview that lists tracked exercises with latest weight and a compact trend indicator.
- Provide a dedicated per-exercise progress chart screen with a selectable recent time range.

## Out Of Scope
- Body muscle visualization graphics.
- Social features or sharing.
- Ads.
- Authentication/login/cloud account.
- Wearables integration.
- Coach programming import/export.
- Broader `Programs` tabs such as `Program`, `Weights`, and `Sets×Reps` until their scope is explicitly defined; they remain hidden on the current branch.
- Rich coaching or media content for the `Form` tab beyond basic structure/navigation.
- History `Notes` workflows until their scope is explicitly defined.

## UX Direction
- Keep UI visually and structurally close to StrongLifts-style training apps:
- Prominent current set action.
- Minimal clutter.
- Fast one-handed flow.
- Functional behavior correctness is higher priority than pixel-perfect cloning.
- Front page shows only production features; dev/debug actions (such as persistence demos or catalog debug buttons) are not visible in the production UX.
- Navigation follows standard Android patterns:
  - Top app bars with expected back/up behavior.
  - Nested edit/detail flows use back/up navigation rather than opening as top-level destinations.
- Core areas are exposed via a bottom navigation bar with icons for `Home`, `Programs`, `History`, `Progress`, and `Settings`, consistent across primary screens.
- The bottom navigation bar is not shown during the focused active-workout flow or other nested detail/edit screens.
- Lists and collections use native Android patterns (row actions, overflow/context menus, swipe actions where appropriate) instead of oversized inline Edit/Delete buttons.
- Home and top-level browsing screens should follow the reference pack:
  - Home shows upcoming workout cards and a prominent `Start Workout` FAB. The exact long-term scheduling logic behind the queue is still not committed beyond a simple deterministic ordering heuristic.
  - `Programs` is the entry point for workout definitions, but the current branch commits only the `Workouts` surface. Broader `Programs` tabs remain deferred and hidden.
  - `History` uses local `List` and `Calendar` sections for the current bounded scope. `Notes` remains deferred until its behavior is explicitly defined.
  - `Progress` shows a scrollable list of exercise trend rows and supports drill-down into a single exercise.
  - `Settings` is a standard top-level form for training defaults rather than a deeper settings architecture.
- Active workout UI should follow the interaction and layout cues in `docs/images/workout_in_progress_0.png`, `docs/images/workout_in_progress_1.png`, `docs/images/workout_in_progress_2.png`, and `docs/images/workout_warmup.png`, including:
  - A tab/switcher between `Workout` and `Warmup` for the current session.
  - Completing a set does not hide or remove its information.
  - Support for partial reps (for example 4 of 5) via tap-to-complete and tap-to-decrement on a gray set circle.
  - Long-press on a set opens a contextual menu for per-set actions (set weight, set reps, clear set, etc.).
  - A trailing `+` action can add extra sets inline after the existing set prescription.
  - Rest countdown feedback appears as a transient bottom overlay rather than a blocking full-screen interruption.
  - The current set and set-circle controls stay visually primary; helper stats, the local section switcher, and timer/status feedback stay secondary and should preserve one-handed progression through the session.
- Workout planning and exercise selection:
  - Planning lists show only the exercises selected for a workout, not the entire catalog inline.
  - An Add exercise action opens a search/scroll picker of exercises not already selected.
  - Duplicate exercises within the same workout are disallowed.
  - Add/selection controls have clear spacing; avoid cramped multi-button layouts.
- The current bounded planning flow keeps minimum slot editing local inside the workout editor; the richer `edit_exercise` reference remains future work.
- Core Home, Programs, Active Workout, and Summary actions should remain usable with TalkBack/keyboard via meaningful labels and stable focus order.
- Empty and recoverable error states stay inside the normal screen shell with contextual next or retry actions rather than separate fallback pages.
- Emphasize Android-native patterns and Material components over custom, button-heavy layouts.

## User Stories
- As a user, I want to create and save my own workouts so I can run my own program.
- As a user, I want to complete any set with one tap so I can stay focused during training.
- As a user, I want rest-end notifications so I do not waste time between sets.
- As a user, I want to start saved workouts quickly so I do not rebuild sessions each time.
- As a user, I want guided warmups and progression rules so I can train safely and progress consistently.

## Acceptance Criteria
- Workouts are persisted across app restarts.
- Workout summaries are persisted across app restarts.
- User can create at least 3 different workouts and run any of them.
- App exposes `Home`, `Programs`, `History`, `Progress`, and `Settings` as the primary top-level navigation areas.
- Home shows upcoming workouts and a quick-start action for the next planned session.
- During an active workout, app presents guided exercise flow with local `Warmup` and `Workout` sections for the current session.
- User can mark any warmup/work set complete at any time with one tap.
- User can add extra sets during an active workout.
- Rest timer defaults to 3 minutes, is configurable, and plays a sound when done.
- Warmup sets are auto-generated deterministically from working weight and match the heuristic rules.
- Progression configuration (mode, increment, deload percent) is stored per exercise slot, with sensible defaults applied only when new slots are created.
- Settings exposes training defaults for rest duration, increment, deload percent, and progression mode, and those values prefill only new sessions or new workout slots.
- Deload is triggered after 3 consecutive failed workouts per exercise using configured percent (default 10%).
- Workout summary displays reps per set, total volume, and elapsed time at completion.
- History provides list and calendar views of completed workouts.
- Progress provides both an overview list and a per-exercise chart view.
- Screen remains awake during active workout.
- Core controls on Home, Programs, Active Workout, and Summary have meaningful accessibility labels and stable keyboard/TalkBack flow.
- Empty and error states preserve the normal screen shell and provide contextual retry or next actions.
