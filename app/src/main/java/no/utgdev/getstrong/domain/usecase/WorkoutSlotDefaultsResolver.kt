package no.utgdev.getstrong.domain.usecase

import javax.inject.Inject
import no.utgdev.getstrong.domain.model.ProgressionModeCode

data class WorkoutSlotDefaults(
    val targetSets: Int,
    val targetReps: Int,
    val repRangeMin: Int,
    val repRangeMax: Int,
    val progressionMode: String,
    val incrementKg: Double,
    val deloadPercent: Int,
)

class WorkoutSlotDefaultsResolver @Inject constructor() {
    fun resolve(
        exerciseName: String,
        defaultProgressionMode: String,
        defaultIncrementKg: Double,
        defaultDeloadPercent: Int,
    ): WorkoutSlotDefaults {
        val isDeadlift = exerciseName.contains("deadlift", ignoreCase = true)
        return if (isDeadlift) {
            WorkoutSlotDefaults(
                targetSets = 1,
                targetReps = 5,
                repRangeMin = 5,
                repRangeMax = 5,
                progressionMode = normalizeProgressionMode(defaultProgressionMode),
                incrementKg = defaultIncrementKg,
                deloadPercent = defaultDeloadPercent,
            )
        } else {
            WorkoutSlotDefaults(
                targetSets = 5,
                targetReps = 5,
                repRangeMin = 5,
                repRangeMax = 5,
                progressionMode = normalizeProgressionMode(defaultProgressionMode),
                incrementKg = defaultIncrementKg,
                deloadPercent = defaultDeloadPercent,
            )
        }
    }

    private fun normalizeProgressionMode(mode: String): String =
        when (mode) {
            ProgressionModeCode.WEIGHT_ONLY,
            ProgressionModeCode.REPS_ONLY,
            ProgressionModeCode.REPS_THEN_WEIGHT,
            -> mode
            else -> ProgressionModeCode.WEIGHT_ONLY
        }
}
