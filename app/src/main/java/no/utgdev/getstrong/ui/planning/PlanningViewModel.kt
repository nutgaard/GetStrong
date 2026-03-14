package no.utgdev.getstrong.ui.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.utgdev.getstrong.domain.repository.SessionRepository
import no.utgdev.getstrong.domain.repository.SettingsRepository
import no.utgdev.getstrong.domain.repository.WorkoutRepository
import no.utgdev.getstrong.domain.usecase.StartWorkoutSessionUseCase

@HiltViewModel
class PlanningViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: SessionRepository,
    private val startWorkoutSessionUseCase: StartWorkoutSessionUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlanningUiState())
    val uiState: StateFlow<PlanningUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val workouts = workoutRepository.getAllWorkouts()
                val settings = settingsRepository.settings.first()
                val unfinishedSessionId = sessionRepository.findUnfinishedSessionId()
                val unfinishedSessionWorkoutId = unfinishedSessionId
                    ?.let { sessionId -> sessionRepository.getActiveSessionState(sessionId)?.session?.workoutId }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        workouts = workouts,
                        trainingDays = settings.trainingDays,
                        unfinishedSessionId = unfinishedSessionId,
                        unfinishedSessionWorkoutId = unfinishedSessionWorkoutId,
                    )
                }
            } catch (_: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not load workouts. Please try again.",
                        workouts = emptyList(),
                        unfinishedSessionId = null,
                        unfinishedSessionWorkoutId = null,
                    )
                }
            }
        }
    }

    fun toggleTrainingDay(day: Int) {
        val previousDays = _uiState.value.trainingDays
        val updatedDays = if (previousDays.contains(day)) {
            previousDays - day
        } else {
            previousDays + day
        }.distinct().sorted()
        _uiState.update { it.copy(trainingDays = updatedDays, scheduleErrorMessage = null) }
        viewModelScope.launch {
            try {
                settingsRepository.updateTrainingDays(updatedDays)
            } catch (_: Throwable) {
                _uiState.update {
                    it.copy(
                        trainingDays = previousDays,
                        scheduleErrorMessage = "Could not save schedule changes. Try again.",
                    )
                }
            }
        }
    }

    fun deleteWorkout(workoutId: Long) {
        viewModelScope.launch {
            workoutRepository.deleteWorkout(workoutId)
            refresh()
        }
    }

    suspend fun startWorkoutSession(workoutId: Long): Long? =
        startWorkoutSessionUseCase(workoutId)
}
