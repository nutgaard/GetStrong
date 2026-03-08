package no.utgdev.getstrong.ui.planning

import no.utgdev.getstrong.domain.model.Workout

data class PlanningUiState(
    val workouts: List<Workout> = emptyList(),
)
