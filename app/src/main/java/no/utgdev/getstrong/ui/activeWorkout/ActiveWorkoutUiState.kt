package no.utgdev.getstrong.ui.activeWorkout

import no.utgdev.getstrong.domain.model.SessionPlannedSet

data class ActiveWorkoutUiState(
    val sessionId: Long = 0,
    val isLoaded: Boolean = false,
    val plannedSets: List<SessionPlannedSet> = emptyList(),
    val currentSet: SessionPlannedSet? = null,
    val isCompleted: Boolean = false,
    val highlightedSetId: Long? = null,
)
