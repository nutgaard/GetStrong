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
    fun resolve(exerciseName: String): WorkoutSlotDefaults {
        val isDeadlift = exerciseName.contains("deadlift", ignoreCase = true)
        return if (isDeadlift) {
            WorkoutSlotDefaults(
                targetSets = 1,
                targetReps = 5,
                repRangeMin = 5,
                repRangeMax = 5,
                progressionMode = ProgressionModeCode.WEIGHT_ONLY,
                incrementKg = 2.5,
                deloadPercent = 10,
            )
        } else {
            WorkoutSlotDefaults(
                targetSets = 5,
                targetReps = 5,
                repRangeMin = 5,
                repRangeMax = 5,
                progressionMode = ProgressionModeCode.WEIGHT_ONLY,
                incrementKg = 2.5,
                deloadPercent = 10,
            )
        }
    }
}
