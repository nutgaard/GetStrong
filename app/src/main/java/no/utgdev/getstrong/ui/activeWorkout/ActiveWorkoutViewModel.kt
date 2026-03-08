package no.utgdev.getstrong.ui.activeWorkout

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
import no.utgdev.getstrong.domain.repository.SessionRepository
import no.utgdev.getstrong.ui.navigation.AppDestination

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private val sessionId =
        savedStateHandle.get<String>(AppDestination.ActiveWorkout.SESSION_ID_ARG)?.toLongOrNull() ?: 0L

    init {
        loadSession()
    }

    fun loadSession() {
        viewModelScope.launch {
            val sessionState = sessionRepository.getActiveSessionState(sessionId)
            _uiState.update {
                it.copy(
                    sessionId = sessionId,
                    isLoaded = true,
                    plannedSets = sessionState?.plannedSets.orEmpty(),
                    currentSet = sessionState?.currentSet,
                    isCompleted = sessionState?.isCompleted == true,
                )
            }
        }
    }

    fun completeCurrentSet() {
        val current = _uiState.value.currentSet ?: return
        viewModelScope.launch {
            val sessionState = sessionRepository.completePlannedSet(
                sessionId = sessionId,
                plannedSetId = current.id,
                repsAchieved = current.targetReps,
            )
            _uiState.update {
                it.copy(
                    plannedSets = sessionState?.plannedSets.orEmpty(),
                    currentSet = sessionState?.currentSet,
                    isCompleted = sessionState?.isCompleted == true,
                )
            }
        }
    }

    fun finishSession() {
        viewModelScope.launch {
            sessionRepository.completeSession(sessionId)
            loadSession()
        }
    }
}
