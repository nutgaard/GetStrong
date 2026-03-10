package no.utgdev.getstrong.ui.planning

import no.utgdev.getstrong.domain.model.Workout

data class PlanningUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val workouts: List<Workout> = emptyList(),
    val trainingDays: List<Int> = emptyList(),
    val scheduleErrorMessage: String? = null,
)
