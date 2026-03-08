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
    val workoutSlotId: Long?,
    val exerciseId: Long,
    val setType: String,
    val reps: Int,
    val weightKg: Double,
)

data class SessionPlannedSet(
    val id: Long = 0,
    val sessionId: Long,
    val workoutSlotId: Long,
    val setOrder: Int,
    val exerciseId: Long,
    val setType: String,
    val targetReps: Int,
    val isCompleted: Boolean = false,
    val completedReps: Int? = null,
)

data class ActiveSessionState(
    val session: WorkoutSession,
    val plannedSets: List<SessionPlannedSet>,
) {
    val currentSet: SessionPlannedSet?
        get() = plannedSets.firstOrNull { !it.isCompleted }

    val isCompleted: Boolean
        get() = plannedSets.isNotEmpty() && plannedSets.all { it.isCompleted }
}

object SessionSetType {
    const val WARMUP = "WARMUP"
    const val WORK = "WORK"
}
