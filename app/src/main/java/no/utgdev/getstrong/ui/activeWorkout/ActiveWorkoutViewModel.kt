package no.utgdev.getstrong.ui.activeWorkout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.repository.SettingsRepository
import no.utgdev.getstrong.domain.time.TimeProvider
import no.utgdev.getstrong.domain.repository.SessionRepository
import no.utgdev.getstrong.domain.usecase.CompleteSessionWithProgressionUseCase
import no.utgdev.getstrong.domain.usecase.RestTimerCalculator
import no.utgdev.getstrong.domain.usecase.RestTimerPolicy
import no.utgdev.getstrong.ui.navigation.AppDestination

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository,
    private val completeSessionWithProgression: CompleteSessionWithProgressionUseCase,
    private val restTimerPolicy: RestTimerPolicy,
    private val restTimerCalculator: RestTimerCalculator,
    private val restSignalPlayer: RestSignalPlayer,
    private val timeProvider: TimeProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()
    private var restTimerJob: Job? = null
    private val restTimerEndAtKey = "rest_timer_end_at_ms"
    private val restTimerTargetSetIdKey = "rest_timer_target_set_id"

    private val sessionId =
        savedStateHandle.get<String>(AppDestination.ActiveWorkout.SESSION_ID_ARG)?.toLongOrNull() ?: 0L

    init {
        observeSettings()
        restoreTimerIfActive()
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
            val completedSet = _uiState.value.plannedSets.firstOrNull { it.id == plannedSetId }
            val sessionState = sessionRepository.completePlannedSet(
                sessionId = sessionId,
                plannedSetId = plannedSetId,
                repsAchieved = repsAchieved,
            )
            maybeStartRestTimer(
                completedSet = completedSet,
                nextCurrentSet = sessionState?.currentSet,
            )
            applySessionState(sessionState, highlightedSetId = plannedSetId)
        }
    }

    fun finishSession() {
        viewModelScope.launch {
            completeSessionWithProgression(sessionId)
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

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                _uiState.update { it.copy(restDurationSeconds = settings.restDurationSeconds) }
            }
        }
    }

    private fun maybeStartRestTimer(
        completedSet: SessionPlannedSet?,
        nextCurrentSet: SessionPlannedSet?,
    ) {
        if (!restTimerPolicy.shouldStartForTransition(completedSet, nextCurrentSet)) return
        val existingTargetSetId = savedStateHandle.get<Long>(restTimerTargetSetIdKey)
        if (_uiState.value.isRestTimerActive && existingTargetSetId == nextCurrentSet?.id) {
            return
        }
        val durationSeconds = _uiState.value.restDurationSeconds
        if (durationSeconds <= 0) return
        val nowMs = timeProvider.nowMs()
        val endAtMs = nowMs + durationSeconds * 1000L
        savedStateHandle[restTimerEndAtKey] = endAtMs
        savedStateHandle[restTimerTargetSetIdKey] = nextCurrentSet?.id
        _uiState.update {
            it.copy(
                restRemainingSeconds = durationSeconds,
                isRestTimerActive = true,
                isRestOver = false,
            )
        }
        startRestTicker(endAtMs)
    }

    private fun restoreTimerIfActive() {
        val endAtMs = savedStateHandle.get<Long>(restTimerEndAtKey) ?: return
        val remaining = restTimerCalculator.remainingSeconds(timeProvider.nowMs(), endAtMs)
        if (remaining <= 0) {
            _uiState.update { it.copy(restRemainingSeconds = 0, isRestTimerActive = false, isRestOver = true) }
            return
        }
        _uiState.update { it.copy(restRemainingSeconds = remaining, isRestTimerActive = true, isRestOver = false) }
        startRestTicker(endAtMs)
    }

    private fun startRestTicker(endAtMs: Long) {
        restTimerJob?.cancel()
        restTimerJob = viewModelScope.launch {
            while (true) {
                val remaining = restTimerCalculator.remainingSeconds(timeProvider.nowMs(), endAtMs)
                _uiState.update {
                    it.copy(
                        restRemainingSeconds = remaining,
                        isRestTimerActive = remaining > 0,
                        isRestOver = remaining == 0,
                    )
                }
                if (remaining <= 0) {
                    restSignalPlayer.playRestOverSignal()
                    savedStateHandle.remove<Long>(restTimerEndAtKey)
                    savedStateHandle.remove<Long>(restTimerTargetSetIdKey)
                    break
                }
                delay(1000L)
            }
        }
    }
}
