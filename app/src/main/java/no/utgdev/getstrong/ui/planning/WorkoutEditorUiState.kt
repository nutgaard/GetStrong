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
    val repRangeMin: Int,
    val repRangeMax: Int,
    val progressionMode: String,
    val incrementKg: Double,
    val deloadPercent: Int,
    val currentWorkingWeightKg: Double,
    val failureStreak: Int,
    val lastProgressionSessionId: Long? = null,
    val restSecondsOverride: Int?,
)
