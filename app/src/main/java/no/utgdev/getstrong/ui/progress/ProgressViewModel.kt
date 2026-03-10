package no.utgdev.getstrong.ui.progress

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

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val sessionSummaryRepository: SessionSummaryRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val exerciseNames = exerciseRepository.getAll().associate { exercise ->
                    exercise.id to exercise.name
                }
                val exercises =
                    sessionSummaryRepository
                        .getAllExerciseHistory()
                        .groupBy { it.exerciseId }
                        .mapNotNull { (exerciseId, entries) ->
                            val latestEntry = latestProgressEntry(entries) ?: return@mapNotNull null
                            val trend = buildSessionBestProgressPoints(entries)
                            ProgressExerciseOverviewUi(
                                exerciseId = exerciseId,
                                exerciseName = exerciseNames[exerciseId] ?: "Exercise $exerciseId",
                                latestWeightKg = latestEntry.weightKg,
                                latestReps = latestEntry.reps,
                                latestEstimatedOneRepMaxKg = latestEntry.estimatedOneRepMaxKg,
                                latestCompletedAtEpochMs = latestEntry.completedAtEpochMs,
                                trendPoints = trend.takeLast(6).map { it.estimatedOneRepMaxKg },
                            )
                        }
                        .sortedWith(
                            compareByDescending<ProgressExerciseOverviewUi> { it.latestCompletedAtEpochMs }
                                .thenBy { it.exerciseName },
                        )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        exercises = exercises,
                    )
                }
            } catch (_: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not load progress. Please try again.",
                        exercises = emptyList(),
                    )
                }
            }
        }
    }
}
