package no.utgdev.getstrong.ui.activeWorkout

import no.utgdev.getstrong.domain.model.SessionPlannedSet

data class ActiveWorkoutUiState(
    val sessionId: Long = 0,
    val isLoaded: Boolean = false,
    val isSessionActive: Boolean = false,
    val plannedSets: List<SessionPlannedSet> = emptyList(),
    val currentSet: SessionPlannedSet? = null,
    val isCompleted: Boolean = false,
    val highlightedSetId: Long? = null,
    val restDurationSeconds: Int = 180,
    val restRemainingSeconds: Int = 0,
    val isRestTimerActive: Boolean = false,
    val isRestOver: Boolean = false,
)
