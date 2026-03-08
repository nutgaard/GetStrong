package no.utgdev.getstrong.domain.usecase

import no.utgdev.getstrong.domain.model.ProgressionModeCode
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutSlotDefaultsResolverTest {
    private val resolver = WorkoutSlotDefaultsResolver()

    @Test
    fun deadliftDefaultsToOneByFive() {
        val defaults = resolver.resolve("Conventional Deadlift")
        assertEquals(1, defaults.targetSets)
        assertEquals(5, defaults.targetReps)
        assertEquals(ProgressionModeCode.WEIGHT_ONLY, defaults.progressionMode)
        assertEquals(2.5, defaults.incrementKg, 0.0)
    }

    @Test
    fun nonDeadliftDefaultsToFiveByFive() {
        val defaults = resolver.resolve("Bench Press")
        assertEquals(5, defaults.targetSets)
        assertEquals(5, defaults.targetReps)
        assertEquals(5, defaults.repRangeMin)
        assertEquals(5, defaults.repRangeMax)
        assertEquals(10, defaults.deloadPercent)
    }
}
