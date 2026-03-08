package no.utgdev.getstrong.domain.session

import no.utgdev.getstrong.domain.model.ProgressionModeCode
import no.utgdev.getstrong.domain.model.SessionSetType
import no.utgdev.getstrong.domain.model.Workout
import no.utgdev.getstrong.domain.model.WorkoutExerciseSlot
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutSessionEngineTest {
    @Test
    fun buildSessionPlanPreservesExerciseOrderAndWarmupBeforeWork() {
        val workout = Workout(
            id = 1,
            name = "A/B",
            slots = listOf(
                WorkoutExerciseSlot(
                    id = 10,
                    workoutId = 1,
                    exerciseId = 1006,
                    position = 0,
                    targetSets = 2,
                    targetReps = 5,
                    repRangeMin = 5,
                    repRangeMax = 5,
                    progressionMode = ProgressionModeCode.WEIGHT_ONLY,
                    incrementKg = 2.5,
                    deloadPercent = 10,
                    restSecondsOverride = null,
                ),
                WorkoutExerciseSlot(
                    id = 11,
                    workoutId = 1,
                    exerciseId = 1036,
                    position = 1,
                    targetSets = 1,
                    targetReps = 8,
                    repRangeMin = 8,
                    repRangeMax = 8,
                    progressionMode = ProgressionModeCode.WEIGHT_ONLY,
                    incrementKg = 2.5,
                    deloadPercent = 10,
                    restSecondsOverride = null,
                ),
            ),
        )

        val plan = WorkoutSessionEngine().buildSessionPlan(workout)

        assertEquals(5, plan.size)
        assertEquals(SessionSetType.WARMUP, plan[0].setType)
        assertEquals(1006, plan[0].exerciseId)
        assertEquals(SessionSetType.WORK, plan[1].setType)
        assertEquals(SessionSetType.WORK, plan[2].setType)

        assertEquals(SessionSetType.WARMUP, plan[3].setType)
        assertEquals(1036, plan[3].exerciseId)
        assertEquals(SessionSetType.WORK, plan[4].setType)

        assertEquals(listOf(0, 1, 2, 3, 4), plan.map { it.setOrder })
    }
}
