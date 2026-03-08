package no.utgdev.getstrong.domain.model

data class WorkoutSessionSummary(
    val sessionId: Long,
    val workoutId: Long,
    val totalVolumeKg: Double,
    val totalDurationSeconds: Long,
    val volumeRule: String,
    val sets: List<WorkoutSessionSummarySet>,
)

data class WorkoutSessionSummarySet(
    val setOrder: Int,
    val setType: String,
    val exerciseId: Long,
    val targetReps: Int,
    val achievedReps: Int?,
    val loadKg: Double?,
)
