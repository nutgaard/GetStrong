package no.utgdev.getstrong.ui.progress

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.utgdev.getstrong.domain.repository.ExerciseRepository
import no.utgdev.getstrong.domain.repository.SessionSummaryRepository
import no.utgdev.getstrong.ui.navigation.AppDestination

@HiltViewModel
class ExerciseProgressViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionSummaryRepository: SessionSummaryRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExerciseProgressUiState())
    val uiState: StateFlow<ExerciseProgressUiState> = _uiState.asStateFlow()

    private val exerciseId =
        savedStateHandle.get<String>(AppDestination.ExerciseProgress.EXERCISE_ID_ARG)?.toLongOrNull() ?: 0L

    private var allPoints: List<ExerciseProgressPointUi> = emptyList()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isLoading = true, errorMessage = null, exerciseId = exerciseId)
            }
            try {
                val exerciseName = exerciseRepository.getById(exerciseId)?.name ?: "Exercise $exerciseId"
                allPoints = buildSessionBestProgressPoints(sessionSummaryRepository.getExerciseHistory(exerciseId))
                _uiState.update { state ->
                    buildExerciseProgressState(
                        previous = state,
                        exerciseId = exerciseId,
                        exerciseName = exerciseName,
                        allPoints = allPoints,
                        selectedRange = state.selectedRange,
                    )
                }
            } catch (_: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not load exercise progress. Please try again.",
                        points = emptyList(),
                        latestWeightKg = 0.0,
                        latestReps = 0,
                        bestEstimatedOneRepMaxKg = 0.0,
                        totalSessions = 0,
                    )
                }
            }
        }
    }

    fun selectRange(range: ProgressRangeOption) {
        _uiState.update { state ->
            buildExerciseProgressState(
                previous = state,
                exerciseId = exerciseId,
                exerciseName = state.exerciseName,
                allPoints = allPoints,
                selectedRange = range,
            )
        }
    }
}

private fun buildExerciseProgressState(
    previous: ExerciseProgressUiState,
    exerciseId: Long,
    exerciseName: String,
    allPoints: List<ExerciseProgressPointUi>,
    selectedRange: ProgressRangeOption,
): ExerciseProgressUiState {
    val visiblePoints = filterProgressPoints(allPoints, selectedRange)
    val latestPoint = visiblePoints.lastOrNull() ?: allPoints.lastOrNull()
    val bestEstimatedOneRepMaxKg = visiblePoints.maxOfOrNull { it.estimatedOneRepMaxKg } ?: 0.0

    return previous.copy(
        exerciseId = exerciseId,
        exerciseName = exerciseName,
        isLoading = false,
        errorMessage = null,
        selectedRange = selectedRange,
        points = visiblePoints,
        latestWeightKg = latestPoint?.weightKg ?: 0.0,
        latestReps = latestPoint?.reps ?: 0,
        bestEstimatedOneRepMaxKg = bestEstimatedOneRepMaxKg,
        totalSessions = visiblePoints.size,
    )
}
