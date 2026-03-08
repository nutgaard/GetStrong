package no.utgdev.getstrong.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import no.utgdev.getstrong.domain.model.ProgressionInput
import no.utgdev.getstrong.domain.model.ProgressionResult
import no.utgdev.getstrong.domain.model.ProgressionModeCode

class ProgressionCalculator @Inject constructor() {
    fun calculate(input: ProgressionInput): ProgressionResult {
        val repMin = minOf(input.repRangeMin, input.repRangeMax)
        val repMax = maxOf(input.repRangeMin, input.repRangeMax)
        val currentReps = input.currentTargetReps.coerceIn(repMin, repMax)

        val recentWorkSets = input.completedWorkSetReps.takeLast(input.workSetTargetCount)
        val allSetsHit = recentWorkSets.size >= input.workSetTargetCount && recentWorkSets.all { it >= currentReps }

        return when (input.progressionMode) {
            ProgressionModeCode.WEIGHT_ONLY -> {
                val nextWeight = if (allSetsHit) {
                    normalizeToIncrement(input.currentWorkingWeightKg + input.incrementKg, input.incrementKg)
                } else {
                    normalizeToIncrement(input.currentWorkingWeightKg, input.incrementKg)
                }
                ProgressionResult(nextTargetReps = currentReps, nextWorkingWeightKg = nextWeight)
            }
            ProgressionModeCode.REPS_ONLY -> {
                val nextReps = if (allSetsHit) minOf(repMax, currentReps + 1) else currentReps
                ProgressionResult(nextTargetReps = nextReps, nextWorkingWeightKg = normalizeToIncrement(input.currentWorkingWeightKg, input.incrementKg))
            }
            ProgressionModeCode.REPS_THEN_WEIGHT -> {
                if (!allSetsHit) {
                    ProgressionResult(nextTargetReps = currentReps, nextWorkingWeightKg = normalizeToIncrement(input.currentWorkingWeightKg, input.incrementKg))
                } else if (currentReps < repMax) {
                    ProgressionResult(nextTargetReps = currentReps + 1, nextWorkingWeightKg = normalizeToIncrement(input.currentWorkingWeightKg, input.incrementKg))
                } else {
                    ProgressionResult(
                        nextTargetReps = repMin,
                        nextWorkingWeightKg = normalizeToIncrement(input.currentWorkingWeightKg + input.incrementKg, input.incrementKg),
                    )
                }
            }
            else -> {
                ProgressionResult(nextTargetReps = currentReps, nextWorkingWeightKg = normalizeToIncrement(input.currentWorkingWeightKg, input.incrementKg))
            }
        }
    }

    private fun normalizeToIncrement(value: Double, increment: Double): Double {
        if (increment <= 0.0) return value
        val quotient = BigDecimal.valueOf(value).divide(BigDecimal.valueOf(increment), 10, RoundingMode.HALF_UP)
        val roundedQuotient = quotient.setScale(0, RoundingMode.HALF_UP)
        return roundedQuotient.multiply(BigDecimal.valueOf(increment)).setScale(3, RoundingMode.HALF_UP).toDouble()
    }

    fun applyDeload(currentWorkingWeightKg: Double, deloadPercent: Int, incrementKg: Double): Double {
        val multiplier = ((100 - deloadPercent).coerceIn(0, 100)).toDouble() / 100.0
        return normalizeToIncrement(currentWorkingWeightKg * multiplier, incrementKg)
    }
}
