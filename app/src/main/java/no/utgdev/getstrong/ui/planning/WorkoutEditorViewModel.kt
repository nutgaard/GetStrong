package no.utgdev.getstrong.ui.planning

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
import no.utgdev.getstrong.domain.model.Workout
import no.utgdev.getstrong.domain.model.WorkoutExerciseSlot
import no.utgdev.getstrong.domain.repository.ExerciseRepository
import no.utgdev.getstrong.domain.repository.WorkoutRepository
import no.utgdev.getstrong.domain.usecase.WorkoutSlotDefaultsResolver
import no.utgdev.getstrong.ui.navigation.AppDestination

@HiltViewModel
class WorkoutEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val workoutSlotDefaultsResolver: WorkoutSlotDefaultsResolver,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutEditorUiState())
    val uiState: StateFlow<WorkoutEditorUiState> = _uiState.asStateFlow()

    private val routeWorkoutId: Long? =
        savedStateHandle.get<String>(AppDestination.PlanningEditor.WORKOUT_ID_ARG)
            ?.takeUnless { it == AppDestination.PlanningEditor.NEW_WORKOUT_TOKEN }
            ?.toLongOrNull()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val exercises = exerciseRepository.getAll()
            val existing = routeWorkoutId?.let { workoutRepository.getWorkout(it) }

            val slots = existing?.slots?.sortedBy { it.position }?.map { slot ->
                val exerciseName = exercises.firstOrNull { it.id == slot.exerciseId }?.name ?: "Exercise ${slot.exerciseId}"
                WorkoutSlotDraft(
                    id = slot.id,
                    exerciseId = slot.exerciseId,
                    exerciseName = exerciseName,
                    position = slot.position,
                    targetSets = slot.targetSets,
                    targetReps = slot.targetReps,
                    repRangeMin = slot.repRangeMin,
                    repRangeMax = slot.repRangeMax,
                    progressionMode = slot.progressionMode,
                    incrementKg = slot.incrementKg,
                    deloadPercent = slot.deloadPercent,
                    currentWorkingWeightKg = slot.currentWorkingWeightKg,
                    failureStreak = slot.failureStreak,
                    lastProgressionSessionId = slot.lastProgressionSessionId,
                    restSecondsOverride = slot.restSecondsOverride,
                )
            }.orEmpty()

            _uiState.value =
                WorkoutEditorUiState(
                    workoutId = existing?.id,
                    name = existing?.name.orEmpty(),
                    availableExercises = exercises,
                    slots = slots,
                    isLoaded = true,
                )
        }
    }

    fun setName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun addExercise(exerciseId: Long) {
        val current = _uiState.value
        val exercise = current.availableExercises.firstOrNull { it.id == exerciseId } ?: return
        val nextPosition = current.slots.size
        val defaults = workoutSlotDefaultsResolver.resolve(exercise.name)
        val newSlot = WorkoutSlotDraft(
            id = 0,
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            position = nextPosition,
            targetSets = defaults.targetSets,
            targetReps = defaults.targetReps,
            repRangeMin = defaults.repRangeMin,
            repRangeMax = defaults.repRangeMax,
            progressionMode = defaults.progressionMode,
            incrementKg = defaults.incrementKg,
            deloadPercent = defaults.deloadPercent,
            currentWorkingWeightKg = 0.0,
            failureStreak = 0,
            lastProgressionSessionId = null,
            restSecondsOverride = null,
        )
        _uiState.update { it.copy(slots = it.slots + newSlot) }
    }

    fun removeSlot(slotId: Long, position: Int) {
        _uiState.update { state ->
            val filtered = state.slots.filterNot { it.id == slotId && it.position == position }
            state.copy(slots = filtered.reindexPositions())
        }
    }

    fun moveSlotUp(position: Int) {
        if (position <= 0) return
        _uiState.update { state ->
            val mutable = state.slots.toMutableList()
            val current = mutable.indexOfFirst { it.position == position }
            val previous = mutable.indexOfFirst { it.position == position - 1 }
            if (current < 0 || previous < 0) return@update state
            val temp = mutable[current]
            mutable[current] = mutable[previous]
            mutable[previous] = temp
            state.copy(slots = mutable.reindexPositions())
        }
    }

    fun moveSlotDown(position: Int) {
        _uiState.update { state ->
            if (position >= state.slots.lastIndex) return@update state
            val mutable = state.slots.toMutableList()
            val current = mutable.indexOfFirst { it.position == position }
            val next = mutable.indexOfFirst { it.position == position + 1 }
            if (current < 0 || next < 0) return@update state
            val temp = mutable[current]
            mutable[current] = mutable[next]
            mutable[next] = temp
            state.copy(slots = mutable.reindexPositions())
        }
    }

    suspend fun save(): Long {
        val state = _uiState.value
        val model = Workout(
            id = state.workoutId ?: 0,
            name = state.name.ifBlank { "Untitled Workout" },
            slots = state.slots.reindexPositions().map { slot ->
                WorkoutExerciseSlot(
                    id = slot.id,
                    workoutId = state.workoutId ?: 0,
                    exerciseId = slot.exerciseId,
                    position = slot.position,
                    targetSets = slot.targetSets,
                    targetReps = slot.targetReps,
                    repRangeMin = slot.repRangeMin,
                    repRangeMax = slot.repRangeMax,
                    progressionMode = slot.progressionMode,
                    incrementKg = slot.incrementKg,
                    deloadPercent = slot.deloadPercent,
                    currentWorkingWeightKg = slot.currentWorkingWeightKg,
                    failureStreak = slot.failureStreak,
                    lastProgressionSessionId = slot.lastProgressionSessionId,
                    restSecondsOverride = slot.restSecondsOverride,
                )
            },
        )

        val savedWorkoutId = if (state.workoutId == null) {
            workoutRepository.createWorkout(model)
        } else {
            workoutRepository.updateWorkout(model)
            state.workoutId
        } ?: 0

        _uiState.update { it.copy(workoutId = savedWorkoutId) }
        return savedWorkoutId
    }

    private fun List<WorkoutSlotDraft>.reindexPositions(): List<WorkoutSlotDraft> =
        mapIndexed { idx, slot -> slot.copy(position = idx) }
}
