package no.utgdev.getstrong.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.round
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.utgdev.getstrong.domain.model.SessionSetType
import no.utgdev.getstrong.domain.repository.ExerciseRepository
import no.utgdev.getstrong.domain.repository.SessionSummaryRepository
import no.utgdev.getstrong.domain.repository.WorkoutSummaryRepository

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val workoutSummaryRepository: WorkoutSummaryRepository,
    private val sessionSummaryRepository: SessionSummaryRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val history = workoutSummaryRepository.getHistory()
                val summariesBySession = history.associate { item ->
                    item.sessionId to sessionSummaryRepository.getSessionSummary(item.sessionId)
                }
                val exerciseIds = summariesBySession.values
                    .flatMap { it?.sets.orEmpty() }
                    .map { it.exerciseId }
                    .distinct()
                val exerciseNames = exerciseIds.associateWith { exerciseId ->
                    exerciseRepository.getById(exerciseId)?.name ?: "Exercise $exerciseId"
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        workouts = history.map { item ->
                            HistoryWorkoutCardUi(
                                id = item.id,
                                sessionId = item.sessionId,
                                workoutName = item.workoutName,
                                totalVolumeKg = item.totalVolumeKg,
                                totalDurationSeconds = item.totalDurationSeconds,
                                completedAtEpochMs = item.completedAtEpochMs,
                                exerciseResults = summariesBySession[item.sessionId]
                                    ?.sets
                                    .orEmpty()
                                    .filter { set -> set.setType != SessionSetType.WARMUP }
                                    .groupBy { set -> set.exerciseId }
                                    .entries
                                    .sortedBy { (_, sets) -> sets.minOf { set -> set.setOrder } }
                                    .map { (exerciseId, sets) ->
                                        val bestSet = sets.maxByOrNull { (it.achievedReps ?: 0) * (it.loadKg ?: 0.0) }
                                        val bestWeight = bestSet?.loadKg
                                        val bestReps = bestSet?.achievedReps ?: 0
                                        HistoryExerciseResultUi(
                                            exerciseId = exerciseId,
                                            exerciseName = exerciseNames[exerciseId] ?: "Exercise $exerciseId",
                                            resultSummary = buildExerciseResultSummary(
                                                setCount = sets.size,
                                                bestWeightKg = bestWeight,
                                                bestReps = bestReps,
                                            ),
                                        )
                                    }
                                    .sortedBy { exercise -> exercise.exerciseName },
                            )
                        },
                    )
                }
            } catch (_: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not load workout history. Please try again.",
                        workouts = emptyList(),
                    )
                }
            }
        }
    }

    private fun buildExerciseResultSummary(
        setCount: Int,
        bestWeightKg: Double?,
        bestReps: Int,
    ): String {
        val setsLabel = if (setCount == 1) "1 set" else "$setCount sets"
        if (bestWeightKg == null) return setsLabel
        val normalizedWeight = if (bestWeightKg % 1.0 == 0.0) {
            "${bestWeightKg.toInt()} kg"
        } else {
            "${round(bestWeightKg * 10.0) / 10.0} kg"
        }
        return "$setsLabel • best $normalizedWeight x $bestReps"
    }
}
