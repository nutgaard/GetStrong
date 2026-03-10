package no.utgdev.getstrong.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeAccessibilityLabelTest {
    @Test
    fun buildsUpcomingWorkoutCardDescription() {
        val description = buildUpcomingWorkoutCardDescription(
            HomeUpcomingWorkoutUi(
                workoutId = 7L,
                workoutName = "Workout A",
                scheduledDateIso = "2026-03-10",
                scheduledLabel = "Today",
                exercisePreview = listOf("Squat", "Press"),
                additionalExerciseCount = 1,
                isNextUp = true,
            ),
        )

        assertEquals(
            "Workout A. Scheduled Today. Next up. Upcoming lifts: Squat, Press. Plus 1 more exercises.",
            description,
        )
    }

    @Test
    fun buildsQuickStartDescriptionForNextWorkout() {
        val description = buildQuickStartActionDescription(
            nextWorkout = HomeUpcomingWorkoutUi(
                workoutId = 7L,
                workoutName = "Workout A",
                scheduledDateIso = "2026-03-10",
                scheduledLabel = "Today",
                exercisePreview = emptyList(),
                additionalExerciseCount = 0,
                isNextUp = true,
            ),
            isStarting = false,
        )

        assertEquals("Start workout Workout A", description)
    }
}
