package no.utgdev.getstrong.ui.home

data class HomeUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val startErrorMessage: String? = null,
    val isStartingWorkout: Boolean = false,
    val hasSavedWorkouts: Boolean = false,
    val upcomingWorkouts: List<HomeUpcomingWorkoutUi> = emptyList(),
)

data class HomeUpcomingWorkoutUi(
    val workoutId: Long,
    val workoutName: String,
    val scheduledLabel: String,
    val exercisePreview: List<String>,
    val additionalExerciseCount: Int,
    val isNextUp: Boolean,
)
