package no.utgdev.getstrong.domain.usecase

import no.utgdev.getstrong.domain.model.ProgressionInput
import no.utgdev.getstrong.domain.model.ProgressionModeCode
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressionCalculatorTest {
    private val calculator = ProgressionCalculator()

    @Test
    fun weightOnlyIncreasesWeightWhenAllSetsHitTarget() {
        val result = calculator.calculate(
            ProgressionInput(
                progressionMode = ProgressionModeCode.WEIGHT_ONLY,
                repRangeMin = 5,
                repRangeMax = 5,
                incrementKg = 2.5,
                deloadPercent = 10,
                currentTargetReps = 5,
                currentWorkingWeightKg = 100.0,
                workSetTargetCount = 3,
                completedWorkSetReps = listOf(5, 5, 5),
            ),
        )

        assertEquals(5, result.nextTargetReps)
        assertEquals(102.5, result.nextWorkingWeightKg, 0.0)
    }

    @Test
    fun repsOnlyIncreasesRepsWithinRangeWhenSetsHitTarget() {
        val result = calculator.calculate(
            ProgressionInput(
                progressionMode = ProgressionModeCode.REPS_ONLY,
                repRangeMin = 6,
                repRangeMax = 8,
                incrementKg = 2.5,
                deloadPercent = 10,
                currentTargetReps = 7,
                currentWorkingWeightKg = 80.0,
                workSetTargetCount = 3,
                completedWorkSetReps = listOf(7, 7, 8),
            ),
        )

        assertEquals(8, result.nextTargetReps)
        assertEquals(80.0, result.nextWorkingWeightKg, 0.0)
    }

    @Test
    fun repsThenWeightAtMaxRepsIncreasesWeightAndResetsRepsToMin() {
        val result = calculator.calculate(
            ProgressionInput(
                progressionMode = ProgressionModeCode.REPS_THEN_WEIGHT,
                repRangeMin = 5,
                repRangeMax = 8,
                incrementKg = 1.25,
                deloadPercent = 10,
                currentTargetReps = 8,
                currentWorkingWeightKg = 87.5,
                workSetTargetCount = 2,
                completedWorkSetReps = listOf(8, 8),
            ),
        )

        assertEquals(5, result.nextTargetReps)
        assertEquals(88.75, result.nextWorkingWeightKg, 0.0)
    }

    @Test
    fun roundToIncrementUsesDeterministicHalfUp() {
        val result = calculator.calculate(
            ProgressionInput(
                progressionMode = ProgressionModeCode.WEIGHT_ONLY,
                repRangeMin = 5,
                repRangeMax = 5,
                incrementKg = 2.5,
                deloadPercent = 10,
                currentTargetReps = 5,
                currentWorkingWeightKg = 101.2,
                workSetTargetCount = 3,
                completedWorkSetReps = listOf(1, 1, 1),
            ),
        )

        assertEquals(5, result.nextTargetReps)
        assertEquals(100.0, result.nextWorkingWeightKg, 0.0)
    }
}
