package no.utgdev.getstrong.ui.planning

import no.utgdev.getstrong.domain.model.Exercise

data class WorkoutEditorUiState(
    val workoutId: Long? = null,
    val name: String = "",
    val availableExercises: List<Exercise> = emptyList(),
    val slots: List<WorkoutSlotDraft> = emptyList(),
    val isLoaded: Boolean = false,
)

data class WorkoutSlotDraft(
    val id: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val position: Int,
    val targetSets: Int,
    val targetReps: Int,
    val progressionMode: String,
    val incrementKg: Double,
    val deloadPercent: Int,
    val restSecondsOverride: Int?,
)
