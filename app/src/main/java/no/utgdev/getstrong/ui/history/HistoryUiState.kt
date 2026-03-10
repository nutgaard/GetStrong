package no.utgdev.getstrong.ui.history

data class HistoryUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val workouts: List<HistoryWorkoutCardUi> = emptyList(),
)

data class HistoryWorkoutCardUi(
    val id: Long,
    val sessionId: Long,
    val workoutName: String,
    val totalVolumeKg: Double,
    val totalDurationSeconds: Long,
    val completedAtEpochMs: Long,
    val exerciseResults: List<HistoryExerciseResultUi> = emptyList(),
)

data class HistoryExerciseResultUi(
    val exerciseId: Long,
    val exerciseName: String,
    val resultSummary: String,
)

data class ExerciseHistoryUiState(
    val exerciseId: Long = 0L,
    val exerciseName: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val rows: List<ExerciseHistoryRowUi> = emptyList(),
)

data class ExerciseHistoryRowUi(
    val sessionId: Long,
    val workoutName: String,
    val completedAtEpochMs: Long,
    val reps: Int,
    val weightKg: Double,
    val estimatedOneRepMaxKg: Double,
)
