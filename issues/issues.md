# Issues found in current app

All issues are tied to screenshots. All screenshots should additionally be compared with the provided UI/UX provided in docs/images

- **calendar.png**: 
  - Calendar starts week on Sundays. It should start on Mondays.
  - Having multiple workouts the same day should not change UI
  - Clicking on a day with a workout should take you the the workout summary for that workout. If there are multiple workouts on a given day, go to selection view
- **edit_exercise_missing_functionality.png**:
  - Cannot set weight, or configure rules for progression anyway
- **home_not_clickable.png**:
  - Clicking the workout in the homescreen does nothing. It should start that workout.
- **new_workout_padding.png**:
  - Padding for "New workout" FAB not consistent with other FABs. Should be reusable component.
- **no_warmup_added.png**:
  - There are no warmup sets added when creating a workout. This must be fixed.
- **workout_in_progress_ui_0.png**:
  - Ordering of exercises are counter intuitive. The current working set starts at the bottom, which makes no sense.
- **workout_in_progress_ui_1.png**:
  - Completing a working set makes the UI float into eachother. Must likely due to the timer pushing the UI.

Additional comments:
- If a workout is started, the state of it should not be deleted when going back to any other screen. It should be persistent until the exersice is completed. The exception is if no sets in the workout has been completed, then it can just be deleted.
- "Workout Actions" still exist even though I've explicitly asked for it to be replaced with a "three dot" context menu, as the orignal images show.
- User should be able to configure a "schedule", e.g I workout on these days, and "up next" workouts should rotate throught the configured workouts and placing them in the next days. making it clear what the plan is for the next 7 days.