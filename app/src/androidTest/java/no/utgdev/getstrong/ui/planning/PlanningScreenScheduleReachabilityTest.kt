package no.utgdev.getstrong.ui.planning

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import no.utgdev.getstrong.domain.model.ProgressionModeCode
import no.utgdev.getstrong.domain.model.Workout
import no.utgdev.getstrong.domain.model.WorkoutExerciseSlot
import org.junit.Rule
import org.junit.Test

class PlanningScreenScheduleReachabilityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun scheduleSectionIsDisplayedWhenWorkoutListHasManyItems() {
        val workouts = (1L..30L).map { workoutId ->
            Workout(
                id = workoutId,
                name = "Workout $workoutId",
                slots = listOf(
                    WorkoutExerciseSlot(
                        id = workoutId,
                        workoutId = workoutId,
                        exerciseId = 1000L + workoutId,
                        position = 0,
                        targetSets = 5,
                        targetReps = 5,
                        repRangeMin = 5,
                        repRangeMax = 5,
                        progressionMode = ProgressionModeCode.WEIGHT_ONLY,
                        incrementKg = 2.5,
                        deloadPercent = 10,
                        currentWorkingWeightKg = 100.0,
                        failureStreak = 0,
                        restSecondsOverride = null,
                    ),
                ),
            )
        }

        composeRule.setContent {
            PlanningScreen(
                uiState = PlanningUiState(
                    isLoading = false,
                    errorMessage = null,
                    workouts = workouts,
                    trainingDays = listOf(1, 3, 5),
                    scheduleErrorMessage = null,
                ),
                onCreateWorkout = {},
                onRetryLoad = {},
                onEditWorkout = {},
                onDeleteWorkout = {},
                onStartWorkout = {},
                onToggleTrainingDay = {},
            )
        }

        composeRule.onNodeWithText("Schedule").assertIsDisplayed()
        composeRule.onNodeWithText("Mon").assertIsDisplayed()
    }
}
