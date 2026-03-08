package no.utgdev.getstrong.domain.model

data class WorkoutSession(
    val id: Long = 0,
    val workoutId: Long,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long? = null,
)

data class SetResult(
    val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val setType: String,
    val reps: Int,
    val weightKg: Double,
)
