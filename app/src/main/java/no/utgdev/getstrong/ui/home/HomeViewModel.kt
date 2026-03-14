package no.utgdev.getstrong.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.utgdev.getstrong.domain.model.Workout
import no.utgdev.getstrong.domain.repository.ExerciseRepository
import no.utgdev.getstrong.domain.repository.SessionRepository
import no.utgdev.getstrong.domain.repository.SettingsRepository
import no.utgdev.getstrong.domain.repository.WorkoutRepository
import no.utgdev.getstrong.domain.repository.WorkoutSummaryRepository
import no.utgdev.getstrong.domain.usecase.StartWorkoutSessionUseCase

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: SessionRepository,
    private val workoutSummaryRepository: WorkoutSummaryRepository,
    private val startWorkoutSessionUseCase: StartWorkoutSessionUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    startErrorMessage = null,
                )
            }
            try {
                val workouts = workoutRepository.getAllWorkouts()
                val exerciseNames = exerciseRepository.getAll().associate { exercise ->
                    exercise.id to exercise.name
                }
                val trainingDays = settingsRepository.settings.first().trainingDays
                val unfinishedSessionId = sessionRepository.findUnfinishedSessionId()
                val unfinishedSessionWorkoutId = unfinishedSessionId
                    ?.let { sessionId -> sessionRepository.getActiveSessionState(sessionId)?.session?.workoutId }
                val recentSummaries = workoutSummaryRepository.getAllSummaries()
                val actionable = buildUpcomingWorkouts(
                    workouts = workouts,
                    exerciseNamesById = exerciseNames,
                    trainingDays = trainingDays,
                    unfinishedWorkoutId = unfinishedSessionWorkoutId,
                    lastCompletedWorkoutId = recentSummaries.maxByOrNull { it.completedAtEpochMs }?.workoutId,
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        startErrorMessage = null,
                        hasSavedWorkouts = workouts.isNotEmpty(),
                        unfinishedSessionId = unfinishedSessionId,
                        upcomingWorkouts = actionable,
                    )
                }
            } catch (_: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Couldn't load your upcoming workouts.",
                        startErrorMessage = null,
                        hasSavedWorkouts = false,
                        unfinishedSessionId = null,
                        upcomingWorkouts = emptyList(),
                    )
                }
            }
        }
    }

    suspend fun startQuickWorkout(): Long? {
        val nextWorkout = _uiState.value.upcomingWorkouts.firstOrNull() ?: return null
        return startWorkout(nextWorkout.workoutId)
    }

    suspend fun startWorkout(workoutId: Long): Long? {
        _uiState.update { it.copy(isStartingWorkout = true, startErrorMessage = null) }
        return try {
            val sessionId = startWorkoutSessionUseCase(workoutId)
            if (sessionId == null) {
                _uiState.update {
                    it.copy(
                        isStartingWorkout = false,
                        startErrorMessage = "Couldn't start the next workout. Try again.",
                    )
                }
                null
            } else {
                _uiState.update { it.copy(isStartingWorkout = false, startErrorMessage = null) }
                sessionId
            }
        } catch (_: Throwable) {
            _uiState.update {
                it.copy(
                    isStartingWorkout = false,
                    startErrorMessage = "Couldn't start the next workout. Try again.",
                )
            }
            null
        }
    }
}

private fun buildUpcomingWorkouts(
    workouts: List<Workout>,
    exerciseNamesById: Map<Long, String>,
    trainingDays: List<Int>,
    unfinishedWorkoutId: Long?,
    lastCompletedWorkoutId: Long?,
): List<HomeUpcomingWorkoutUi> {
    val readyWorkouts = workouts.filter { workout -> workout.slots.isNotEmpty() }
    if (readyWorkouts.isEmpty() || trainingDays.isEmpty()) return emptyList()

    val startIndex = when {
        unfinishedWorkoutId != null -> readyWorkouts.indexOfFirst { workout -> workout.id == unfinishedWorkoutId }
            .takeIf { it >= 0 } ?: 0
        else -> lastCompletedWorkoutId
            ?.let { completedId -> readyWorkouts.indexOfFirst { workout -> workout.id == completedId } }
            ?.takeIf { it >= 0 }
            ?.let { index -> (index + 1) % readyWorkouts.size }
            ?: 0
    }

    val ordered = readyWorkouts.rotateFrom(startIndex)
    val trainingDaysSet = trainingDays.toSet()
    var scheduledWorkoutIndex = 0
    val today = LocalDate.now()

    return (0L..6L).mapNotNull { dayOffset ->
        val date = today.plusDays(dayOffset)
        if (!trainingDaysSet.contains(date.dayOfWeek.value)) return@mapNotNull null
        val workout = ordered[scheduledWorkoutIndex % ordered.size]
        scheduledWorkoutIndex += 1
        val slotNames = workout.slots
            .sortedBy { slot -> slot.position }
            .map { slot -> exerciseNamesById[slot.exerciseId] ?: "Exercise ${slot.exerciseId}" }
        HomeUpcomingWorkoutUi(
            workoutId = workout.id,
            workoutName = workout.name,
            scheduledDateIso = date.toString(),
            scheduledLabel = scheduledLabel(date),
            exercisePreview = slotNames.take(3),
            additionalExerciseCount = (slotNames.size - 3).coerceAtLeast(0),
            isNextUp = scheduledWorkoutIndex == 1,
            isResumeCandidate = unfinishedWorkoutId == workout.id && scheduledWorkoutIndex == 1,
        )
    }
}

private fun List<Workout>.rotateFrom(startIndex: Int): List<Workout> =
    if (isEmpty()) {
        emptyList()
    } else {
        drop(startIndex) + take(startIndex)
    }

private fun scheduledLabel(date: LocalDate): String =
    when (date) {
        LocalDate.now() -> "Today"
        LocalDate.now().plusDays(1) -> "Tomorrow"
        else -> date.format(DateTimeFormatter.ofPattern("EEE d MMM"))
    }
