# Issues found in current app

All issues are tied to screenshots. All screenshots should additionally be compared with the provided UI/UX provided in docs/images

- **edit_exercise_progression_bug.png**:
  - Changing progression-type from "weight" to any of the other options do not affect anything. For instance changing it to "Reps" should not show "Increment (kg)", and "Reps -> Weight" should provide other inputs to configure it.
  - Heading: Android-phones typically have a back-button so having a manual "back" seems wrong in terms of android UX.
  - Top menu: switching to form "Weight" to "Progress" or "History" removes top-menu. It should always be present.
  - Top menu: using back-button from "Progress" or "History" screen takes you back to the "Weight" screen, but the menu still highlights the wrong menu-item
  - What is the "Apply slot settings"? This should not be necessary.
- **workout_in_progress.png**:
  - The color highlighting is really unintuitive. It should be;
    - Not completed; just border
    - Completed/Partially completed: filled
    - Next set: Slightly pulsating animation of a really light background-color
  - The warmup progression is not clear at all. It just says; "3 sets x 3resp @ 42.5". But this is not the correct warmup for a back squat of 95.kg. The warmup should be; 5x20, 5x20 5x40, 5x60, 5x80. See warmup_reference.png for how it should look.
- **settings.png**:
  - a popup block of "Settings saved" seems like really bad UX. It makes the "Save button" move etc.
  - "Save Traing Default" is also a really bad title for button. Why cannot it values auto-save?


General: 
- Clicking "home" in bottom menu seems to take you to the "programs" screen after adding a workout. After completing a workout it seems to work as expected again.,
- When a workout is in progress we should get a "Continue workout" insteadof "start workout" fab
- "Return to app" is a really poor button title. Find a better UX approach.
- Completed workouts are not stored in history anymore