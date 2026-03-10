package no.utgdev.getstrong.ui.planning

import no.utgdev.getstrong.domain.model.Workout
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanningAccessibilityLabelTest {
    @Test
    fun buildsWorkoutRowDescription() {
        val description = buildWorkoutRowContentDescription(
            Workout(
                id = 4L,
                name = "Push Day",
                slots = List(3) { index ->
                    no.utgdev.getstrong.domain.model.WorkoutExerciseSlot(
                        id = index.toLong() + 1L,
                        workoutId = 4L,
                        exerciseId = 1000L + index,
                        position = index,
                        targetSets = 3,
                        targetReps = 5,
                        repRangeMin = 5,
                        repRangeMax = 5,
                        progressionMode = "WEIGHT_ONLY",
                        incrementKg = 2.5,
                        deloadPercent = 10,
                        currentWorkingWeightKg = 100.0,
                    )
                },
            ),
        )

        assertEquals("Push Day. 3 exercises.", description)
    }
}
