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
- Workout planning:
- Create, edit, delete multiple named workouts (for example Push/Pull/Legs).
- Add ordered exercises to each workout.
- Persist workouts between sessions.

- Exercise catalog:
- Ship with a built-in catalog of **50 common exercises**.
- Each exercise includes primary/secondary muscle groups and equipment type.
- Catalog is local/offline.

- Workout execution:
- Start a planned workout and be guided in this order:
- Warmup sets for Exercise A
- Work sets for Exercise A
- Warmup sets for Exercise B
- Work sets for Exercise B
- Continue until workout is complete.
- User may mark **any set** (warmup or work set) as completed at any time via one tap.
- Keep screen awake during active workout.

- Progression models:
- Per exercise, support:
- `weight_only`: increase weight when all target reps are achieved.
- `reps_only`: increase reps within configured rep range.
- `reps_then_weight`: increase reps to rep-range max, then increase weight and reset reps to rep-range min.
- Default progression mode: `weight_only`.
- Default increment: `+2.5 kg`.
- Default working-set prescription:
- Most exercises: 5 sets x 5 reps.
- Deadlift: 1 set x 5 reps.
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
- Persist workout summary for later analytics/charts (charts themselves are out of MVP).

## Out Of Scope
- Progress charts and analytics visualization UI.
- Body muscle visualization graphics.
- Social features or sharing.
- Ads.
- Authentication/login/cloud account.
- Wearables integration.
- Coach programming import/export.

## UX Direction
- Keep UI visually and structurally close to StrongLifts-style training apps:
- Prominent current set action.
- Minimal clutter.
- Fast one-handed flow.
- Functional behavior correctness is higher priority than pixel-perfect cloning.

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
- During an active workout, app presents guided exercise flow (warmups then work sets per exercise).
- User can mark any warmup/work set complete at any time with one tap.
- Rest timer defaults to 3 minutes, is configurable, and plays a sound when done.
- Warmup sets are auto-generated deterministically from working weight and match the heuristic rules.
- Progression mode is configurable per exercise, with defaults applied for new exercises.
- Deload is triggered after 3 consecutive failed workouts per exercise using configured percent (default 10%).
- Workout summary displays reps per set, total volume, and elapsed time at completion.
- Screen remains awake during active workout.
