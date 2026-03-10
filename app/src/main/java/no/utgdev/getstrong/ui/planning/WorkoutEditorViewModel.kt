package no.utgdev.getstrong.ui.planning

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.math.max
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.utgdev.getstrong.domain.model.Workout
import no.utgdev.getstrong.domain.model.WorkoutExerciseSlot
import no.utgdev.getstrong.domain.repository.ExerciseRepository
import no.utgdev.getstrong.domain.repository.SettingsRepository
import no.utgdev.getstrong.domain.repository.WorkoutRepository
import no.utgdev.getstrong.domain.usecase.WorkoutSlotDefaultsResolver
import no.utgdev.getstrong.ui.navigation.AppDestination

@HiltViewModel
class WorkoutEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val settingsRepository: SettingsRepository,
    private val workoutSlotDefaultsResolver: WorkoutSlotDefaultsResolver,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutEditorUiState())
    val uiState: StateFlow<WorkoutEditorUiState> = _uiState.asStateFlow()

    private val routeWorkoutId: Long? =
        savedStateHandle.get<String>(AppDestination.WorkoutEditor.WORKOUT_ID_ARG)
            ?.takeUnless { it == AppDestination.WorkoutEditor.NEW_WORKOUT_TOKEN }
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
        _uiState.update { it.copy(name = name, message = null) }
    }

    fun addExercise(exerciseId: Long) {
        val current = _uiState.value
        val exercise = current.availableExercises.firstOrNull { it.id == exerciseId } ?: return
        if (current.slots.any { it.exerciseId == exerciseId }) {
            _uiState.update { it.copy(message = "${exercise.name} is already in this workout.") }
            return
        }
        val nextPosition = current.slots.size
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val defaults = workoutSlotDefaultsResolver.resolve(
                exerciseName = exercise.name,
                defaultProgressionMode = settings.defaultProgressionMode,
                defaultIncrementKg = settings.loadIncrementKg,
                defaultDeloadPercent = settings.deloadPercent,
            )
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
            _uiState.update { it.copy(slots = it.slots + newSlot, message = null) }
        }
    }

    fun removeSlot(slotId: Long, position: Int) {
        _uiState.update { state ->
            val filtered = state.slots.filterNot { it.id == slotId && it.position == position }
            state.copy(slots = filtered.reindexPositions(), message = null)
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
            state.copy(slots = mutable.reindexPositions(), message = null)
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
            state.copy(slots = mutable.reindexPositions(), message = null)
        }
    }

    fun updateSlotTargets(position: Int, targetSets: Int, targetReps: Int) {
        val normalizedSets = targetSets.coerceAtLeast(1)
        val normalizedReps = targetReps.coerceAtLeast(1)
        _uiState.update { state ->
            val updated = state.slots.map { slot ->
                if (slot.position != position) {
                    slot
                } else {
                    slot.copy(
                        targetSets = normalizedSets,
                        targetReps = normalizedReps,
                        repRangeMin = normalizedReps,
                        repRangeMax = normalizedReps,
                    )
                }
            }
            state.copy(slots = updated, message = null)
        }
    }

    fun updateSlotDetail(
        exerciseId: Long,
        targetSets: Int,
        targetReps: Int,
        currentWorkingWeightKg: Double,
        progressionMode: String,
        incrementKg: Double,
        deloadPercent: Int,
    ) {
        val normalizedSets = targetSets.coerceAtLeast(1)
        val normalizedReps = targetReps.coerceAtLeast(1)
        val normalizedWeight = max(0.0, currentWorkingWeightKg)
        val normalizedIncrement = max(0.0, incrementKg)
        val normalizedDeload = deloadPercent.coerceIn(0, 100)
        _uiState.update { state ->
            val updated = state.slots.map { slot ->
                if (slot.exerciseId != exerciseId) {
                    slot
                } else {
                    slot.copy(
                        targetSets = normalizedSets,
                        targetReps = normalizedReps,
                        repRangeMin = normalizedReps,
                        repRangeMax = normalizedReps,
                        currentWorkingWeightKg = normalizedWeight,
                        progressionMode = progressionMode,
                        incrementKg = normalizedIncrement,
                        deloadPercent = normalizedDeload,
                    )
                }
            }
            state.copy(slots = updated, message = null)
        }
    }

    fun getSlotDetail(exerciseId: Long): ExerciseSlotDetailUi? {
        val state = _uiState.value
        val slot = state.slots.firstOrNull { it.exerciseId == exerciseId } ?: return null
        val exercise = state.availableExercises.firstOrNull { it.id == exerciseId }
        return ExerciseSlotDetailUi(
            workoutId = state.workoutId,
            exerciseId = exerciseId,
            exerciseName = slot.exerciseName,
            equipmentType = exercise?.equipmentType ?: "",
            targetSets = slot.targetSets,
            targetReps = slot.targetReps,
            currentWorkingWeightKg = slot.currentWorkingWeightKg,
            progressionMode = slot.progressionMode,
            incrementKg = slot.incrementKg,
            deloadPercent = slot.deloadPercent,
            plateGuidance = buildPlateGuidance(
                equipmentType = exercise?.equipmentType ?: "",
                targetWeightKg = slot.currentWorkingWeightKg,
            ),
        )
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
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

data class ExerciseSlotDetailUi(
    val workoutId: Long?,
    val exerciseId: Long,
    val exerciseName: String,
    val equipmentType: String,
    val targetSets: Int,
    val targetReps: Int,
    val currentWorkingWeightKg: Double,
    val progressionMode: String,
    val incrementKg: Double,
    val deloadPercent: Int,
    val plateGuidance: String,
)

private fun buildPlateGuidance(
    equipmentType: String,
    targetWeightKg: Double,
): String {
    if (targetWeightKg <= 0.0) {
        return "Set a working weight to show plate guidance."
    }
    return if (equipmentType.uppercase() == "BARBELL") {
        val platePerSide = ((targetWeightKg - 20.0) / 2.0).coerceAtLeast(0.0)
        if (platePerSide <= 0.0) {
            "Use the empty bar."
        } else {
            "Use the 20kg bar plus ${formatLoad(platePerSide)} per side."
        }
    } else {
        "Set machine/dumbbell load to ${formatLoad(targetWeightKg)}."
    }
}

private fun formatLoad(weightKg: Double): String =
    if (weightKg % 1.0 == 0.0) {
        "${weightKg.toInt()}kg"
    } else {
        "${"%.1f".format(weightKg)}kg"
    }
