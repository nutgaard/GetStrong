package no.utgdev.getstrong.domain.usecase

import no.utgdev.getstrong.domain.model.ProgressionModeCode
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutSlotDefaultsResolverTest {
    private val resolver = WorkoutSlotDefaultsResolver()

    @Test
    fun deadliftDefaultsToOneByFive() {
        val defaults = resolver.resolve(
            exerciseName = "Conventional Deadlift",
            defaultProgressionMode = ProgressionModeCode.REPS_THEN_WEIGHT,
            defaultIncrementKg = 1.0,
            defaultDeloadPercent = 8,
        )
        assertEquals(1, defaults.targetSets)
        assertEquals(5, defaults.targetReps)
        assertEquals(ProgressionModeCode.REPS_THEN_WEIGHT, defaults.progressionMode)
        assertEquals(1.0, defaults.incrementKg, 0.0)
    }

    @Test
    fun nonDeadliftDefaultsToFiveByFive() {
        val defaults = resolver.resolve(
            exerciseName = "Bench Press",
            defaultProgressionMode = ProgressionModeCode.WEIGHT_ONLY,
            defaultIncrementKg = 2.5,
            defaultDeloadPercent = 10,
        )
        assertEquals(5, defaults.targetSets)
        assertEquals(5, defaults.targetReps)
        assertEquals(5, defaults.repRangeMin)
        assertEquals(5, defaults.repRangeMax)
        assertEquals(10, defaults.deloadPercent)
    }
}
