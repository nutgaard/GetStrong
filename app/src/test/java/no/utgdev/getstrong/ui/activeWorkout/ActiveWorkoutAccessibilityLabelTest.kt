package no.utgdev.getstrong.ui.activeWorkout

import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.SessionSetType
import org.junit.Assert.assertEquals
import org.junit.Test

class ActiveWorkoutAccessibilityLabelTest {
    @Test
    fun buildsSetAccessibilityDescription() {
        val description = buildSetAccessibilityDescription(
            exerciseName = "Bench Press",
            set = SessionPlannedSet(
                id = 1L,
                sessionId = 9L,
                workoutSlotId = 11L,
                setOrder = 0,
                exerciseId = 1001L,
                setType = SessionSetType.WORK,
                targetReps = 5,
                targetWeightKg = 100.0,
                completedReps = 4,
                isCompleted = true,
            ),
            setIndex = 1,
            setCount = 3,
            isCurrent = true,
        )

        assertEquals(
            "Bench Press. Workout set 1 of 3. Current set. Target 5 reps at 100 kg. Achieved 4 reps.",
            description,
        )
    }

    @Test
    fun buildsSetStateDescription() {
        val state = buildSetAccessibilityStateDescription(
            set = SessionPlannedSet(
                id = 1L,
                sessionId = 9L,
                workoutSlotId = 11L,
                setOrder = 0,
                exerciseId = 1001L,
                setType = SessionSetType.WARMUP,
                targetReps = 5,
                completedReps = 0,
                isCompleted = false,
            ),
            isCurrent = false,
        )

        assertEquals("Incomplete", state)
    }
}
