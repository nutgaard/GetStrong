package no.utgdev.getstrong.domain.model

data class ExerciseHistoryEntry(
    val exerciseId: Long,
    val sessionId: Long,
    val workoutName: String,
    val completedAtEpochMs: Long,
    val reps: Int,
    val weightKg: Double,
    val estimatedOneRepMaxKg: Double,
)
