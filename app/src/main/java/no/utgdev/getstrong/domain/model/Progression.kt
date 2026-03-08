package no.utgdev.getstrong.domain.model

data class ProgressionInput(
    val progressionMode: String,
    val repRangeMin: Int,
    val repRangeMax: Int,
    val incrementKg: Double,
    val deloadPercent: Int,
    val currentTargetReps: Int,
    val currentWorkingWeightKg: Double,
    val workSetTargetCount: Int,
    val completedWorkSetReps: List<Int>,
)

data class ProgressionResult(
    val nextTargetReps: Int,
    val nextWorkingWeightKg: Double,
)

data class SlotProgressionUpdate(
    val slotId: Long,
    val nextTargetReps: Int,
    val nextWorkingWeightKg: Double,
    val nextFailureStreak: Int,
)
