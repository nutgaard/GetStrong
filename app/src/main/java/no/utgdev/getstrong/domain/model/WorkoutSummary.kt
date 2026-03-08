package no.utgdev.getstrong.domain.model

data class WorkoutSummary(
    val id: Long = 0,
    val workoutId: Long,
    val sessionId: Long,
    val totalVolumeKg: Double,
    val totalDurationSeconds: Long,
    val completedAtEpochMs: Long,
)
