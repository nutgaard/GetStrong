package no.utgdev.getstrong.ui.history

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
class ExerciseHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionSummaryRepository: SessionSummaryRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExerciseHistoryUiState())
    val uiState: StateFlow<ExerciseHistoryUiState> = _uiState.asStateFlow()

    private val exerciseId =
        savedStateHandle.get<String>(AppDestination.ExerciseHistory.EXERCISE_ID_ARG)?.toLongOrNull() ?: 0L

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, exerciseId = exerciseId) }
            try {
                val exerciseName = exerciseRepository.getById(exerciseId)?.name ?: "Exercise $exerciseId"
                val rows = sessionSummaryRepository.getExerciseHistory(exerciseId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        exerciseId = exerciseId,
                        exerciseName = exerciseName,
                        rows = rows.map { row ->
                            ExerciseHistoryRowUi(
                                sessionId = row.sessionId,
                                workoutName = row.workoutName,
                                completedAtEpochMs = row.completedAtEpochMs,
                                reps = row.reps,
                                weightKg = row.weightKg,
                                estimatedOneRepMaxKg = row.estimatedOneRepMaxKg,
                            )
                        },
                    )
                }
            } catch (_: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not load exercise history. Please try again.",
                        rows = emptyList(),
                    )
                }
            }
        }
    }
}
