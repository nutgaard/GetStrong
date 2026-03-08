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
            applySessionState(sessionState)
        }
    }

    fun completeCurrentSet() {
        val current = _uiState.value.currentSet ?: return
        completeSet(current.id, current.targetReps)
    }

    fun completeSet(plannedSetId: Long, repsAchieved: Int) {
        viewModelScope.launch {
            val sessionState = sessionRepository.completePlannedSet(
                sessionId = sessionId,
                plannedSetId = plannedSetId,
                repsAchieved = repsAchieved,
            )
            applySessionState(sessionState, highlightedSetId = plannedSetId)
        }
    }

    fun finishSession() {
        viewModelScope.launch {
            sessionRepository.completeSession(sessionId)
            loadSession()
        }
    }

    fun focusSet(plannedSetId: Long) {
        _uiState.update { it.copy(highlightedSetId = plannedSetId) }
    }

    private fun applySessionState(
        sessionState: no.utgdev.getstrong.domain.model.ActiveSessionState?,
        highlightedSetId: Long? = null,
    ) {
        _uiState.update {
            it.copy(
                sessionId = sessionId,
                isLoaded = true,
                plannedSets = sessionState?.plannedSets.orEmpty(),
                currentSet = sessionState?.currentSet,
                isCompleted = sessionState?.isCompleted == true,
                highlightedSetId = highlightedSetId ?: sessionState?.currentSet?.id,
            )
        }
    }
}
