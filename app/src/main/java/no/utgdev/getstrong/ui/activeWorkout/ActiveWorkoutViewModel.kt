package no.utgdev.getstrong.ui.activeWorkout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.repository.ExerciseRepository
import no.utgdev.getstrong.domain.repository.SessionRepository
import no.utgdev.getstrong.domain.repository.SettingsRepository
import no.utgdev.getstrong.domain.time.TimeProvider
import no.utgdev.getstrong.domain.usecase.CompleteSessionAndSaveSummaryUseCase
import no.utgdev.getstrong.domain.usecase.ElapsedTimeCalculator
import no.utgdev.getstrong.domain.usecase.RestTimerCalculator
import no.utgdev.getstrong.domain.usecase.RestTimerPolicy
import no.utgdev.getstrong.ui.navigation.AppDestination

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val exerciseRepository: ExerciseRepository,
    private val settingsRepository: SettingsRepository,
    private val completeSessionAndSaveSummary: CompleteSessionAndSaveSummaryUseCase,
    private val restTimerPolicy: RestTimerPolicy,
    private val restTimerCalculator: RestTimerCalculator,
    private val elapsedTimeCalculator: ElapsedTimeCalculator,
    private val restSignalPlayer: RestSignalPlayer,
    private val timeProvider: TimeProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()
    private var restTimerJob: Job? = null
    private var elapsedTimerJob: Job? = null
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

    fun onSetTapped(plannedSetId: Long) {
        val set = _uiState.value.plannedSets.firstOrNull { it.id == plannedSetId } ?: return
        val nextReps = when {
            (set.completedReps ?: 0) <= 0 -> set.targetReps
            else -> (set.completedReps ?: 0) - 1
        }
        completeSet(plannedSetId, nextReps)
    }

    fun completeSet(plannedSetId: Long, repsAchieved: Int) {
        viewModelScope.launch {
            val previousSet = _uiState.value.plannedSets.firstOrNull { it.id == plannedSetId } ?: return@launch
            val sessionState = sessionRepository.completePlannedSet(
                sessionId = sessionId,
                plannedSetId = plannedSetId,
                repsAchieved = repsAchieved,
            )
            if (!previousSet.isCompleted && repsAchieved > 0) {
                maybeStartRestTimer(
                    completedSet = previousSet.copy(isCompleted = true, completedReps = repsAchieved),
                    nextCurrentSet = sessionState?.currentSet,
                )
            }
            applySessionState(sessionState, highlightedSetId = plannedSetId)
        }
    }

    suspend fun finishSession(): Long {
        completeSessionAndSaveSummary(sessionId)
        loadSession()
        return sessionId
    }

    fun updateSetWeight(plannedSetId: Long, weightKg: Double) {
        viewModelScope.launch {
            val sessionState = sessionRepository.updatePlannedSetWeight(
                sessionId = sessionId,
                plannedSetId = plannedSetId,
                weightKg = weightKg,
            )
            applySessionState(sessionState, highlightedSetId = plannedSetId)
        }
    }

    fun clearSet(plannedSetId: Long) {
        completeSet(plannedSetId, 0)
    }

    fun addExtraSet(anchorPlannedSetId: Long) {
        viewModelScope.launch {
            val sessionState = sessionRepository.addExtraSet(sessionId, anchorPlannedSetId)
            applySessionState(sessionState, highlightedSetId = anchorPlannedSetId)
        }
    }

    fun removeExtraSet(plannedSetId: Long) {
        viewModelScope.launch {
            val sessionState = sessionRepository.removeExtraSet(sessionId, plannedSetId)
            applySessionState(sessionState)
        }
    }

    private suspend fun applySessionState(
        sessionState: no.utgdev.getstrong.domain.model.ActiveSessionState?,
        highlightedSetId: Long? = null,
    ) {
        val session = sessionState?.session
        val plannedSets = sessionState?.plannedSets.orEmpty()
        val exerciseNames = resolveExerciseNames(plannedSets)
        updateElapsedTimer(
            startedAtEpochMs = session?.startedAtEpochMs,
            endedAtEpochMs = session?.endedAtEpochMs,
        )
        _uiState.update {
            it.copy(
                sessionId = sessionId,
                isLoaded = true,
                isSessionActive = sessionState?.session?.endedAtEpochMs == null,
                plannedSets = plannedSets,
                exerciseNamesById = exerciseNames,
                currentSet = sessionState?.currentSet,
                isCompleted = sessionState?.isCompleted == true,
                highlightedSetId = highlightedSetId ?: sessionState?.currentSet?.id,
            )
        }
    }

    private fun updateElapsedTimer(startedAtEpochMs: Long?, endedAtEpochMs: Long?) {
        elapsedTimerJob?.cancel()
        if (startedAtEpochMs == null) {
            _uiState.update { it.copy(elapsedSeconds = 0L) }
            return
        }
        if (endedAtEpochMs != null) {
            _uiState.update {
                it.copy(
                    elapsedSeconds = elapsedTimeCalculator.elapsedSeconds(
                        startedAtEpochMs = startedAtEpochMs,
                        endedAtEpochMs = endedAtEpochMs,
                        nowEpochMs = endedAtEpochMs,
                    ),
                )
            }
            return
        }
        elapsedTimerJob = viewModelScope.launch {
            while (true) {
                val nowMs = timeProvider.nowMs()
                _uiState.update {
                    it.copy(
                        elapsedSeconds = elapsedTimeCalculator.elapsedSeconds(
                            startedAtEpochMs = startedAtEpochMs,
                            endedAtEpochMs = null,
                            nowEpochMs = nowMs,
                        ),
                    )
                }
                delay(1000L)
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            _uiState.update { it.copy(restDurationSeconds = settings.restDurationSeconds) }
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
            _uiState.update { it.copy(restRemainingSeconds = 0, isRestTimerActive = false, isRestOver = false) }
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
                    delay(1500L)
                    _uiState.update { it.copy(isRestOver = false) }
                    break
                }
                delay(1000L)
            }
        }
    }

    private suspend fun resolveExerciseNames(plannedSets: List<SessionPlannedSet>): Map<Long, String> {
        val existing = _uiState.value.exerciseNamesById
        val missingIds = plannedSets
            .map { it.exerciseId }
            .distinct()
            .filterNot(existing::containsKey)

        if (missingIds.isEmpty()) return existing

        val loaded = missingIds.associateWith { exerciseId ->
            exerciseRepository.getById(exerciseId)?.name ?: "Exercise $exerciseId"
        }
        return existing + loaded
    }
}
