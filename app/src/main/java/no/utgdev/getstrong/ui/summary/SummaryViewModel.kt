package no.utgdev.getstrong.ui.summary

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
class SummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionSummaryRepository: SessionSummaryRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SummaryUiState())
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    private val sessionId =
        savedStateHandle.get<String>(AppDestination.Summary.SESSION_ID_ARG)?.toLongOrNull() ?: 0L

    init {
        loadSummary()
    }

    fun loadSummary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, sessionId = sessionId) }
            try {
                val summary = sessionSummaryRepository.getSessionSummary(sessionId)
                val exerciseNames = resolveExerciseNames(summary?.sets.orEmpty().map { it.exerciseId })
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = if (summary == null) "Summary unavailable for this session." else null,
                        totalVolumeKg = summary?.totalVolumeKg ?: 0.0,
                        totalDurationSeconds = summary?.totalDurationSeconds ?: 0L,
                        volumeRule = summary?.volumeRule.orEmpty(),
                        sets = summary?.sets.orEmpty().map { row ->
                            SummarySetRowUi(
                                setOrder = row.setOrder,
                                setType = row.setType,
                                exerciseId = row.exerciseId,
                                exerciseName = exerciseNames[row.exerciseId] ?: "Exercise ${row.exerciseId}",
                                targetReps = row.targetReps,
                                achievedReps = row.achievedReps,
                                loadKg = row.loadKg,
                            )
                        },
                    )
                }
            } catch (_: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not load workout summary. Please try again.",
                        totalVolumeKg = 0.0,
                        totalDurationSeconds = 0L,
                        volumeRule = "",
                        sets = emptyList(),
                    )
                }
            }
        }
    }

    private suspend fun resolveExerciseNames(exerciseIds: List<Long>): Map<Long, String> =
        exerciseIds
            .distinct()
            .associateWith { exerciseId ->
                exerciseRepository.getById(exerciseId)?.name ?: "Exercise $exerciseId"
            }
}
