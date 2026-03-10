package no.utgdev.getstrong.ui.planning

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import no.utgdev.getstrong.domain.model.AppSettings
import no.utgdev.getstrong.domain.model.EquipmentTypeCode
import no.utgdev.getstrong.domain.model.Exercise
import no.utgdev.getstrong.domain.model.MuscleGroupCode
import no.utgdev.getstrong.domain.model.ProgressionModeCode
import no.utgdev.getstrong.domain.model.Workout
import no.utgdev.getstrong.domain.repository.ExerciseRepository
import no.utgdev.getstrong.domain.repository.SettingsRepository
import no.utgdev.getstrong.domain.repository.WorkoutRepository
import no.utgdev.getstrong.domain.usecase.WorkoutSlotDefaultsResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.After
import org.junit.Before
import org.junit.Test

class WorkoutEditorViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun addExerciseUsesSettingsBackedProgressionDefaults() = runTest {
        val exerciseRepository = FakeExerciseRepository()
        val workoutRepository = FakeWorkoutRepository()
        val settingsRepository = FakeSettingsRepository(
            AppSettings(
                restDurationSeconds = 180,
                loadIncrementKg = 1.25,
                deloadPercent = 12,
                defaultProgressionMode = ProgressionModeCode.REPS_ONLY,
            ),
        )
        val viewModel = WorkoutEditorViewModel(
            savedStateHandle = SavedStateHandle(),
            workoutRepository = workoutRepository,
            exerciseRepository = exerciseRepository,
            settingsRepository = settingsRepository,
            workoutSlotDefaultsResolver = WorkoutSlotDefaultsResolver(),
        )
        advanceUntilIdle()

        viewModel.addExercise(1001L)
        advanceUntilIdle()

        val slot = viewModel.uiState.value.slots.single()
        assertEquals(ProgressionModeCode.REPS_ONLY, slot.progressionMode)
        assertEquals(1.25, slot.incrementKg, 0.0)
        assertEquals(12, slot.deloadPercent)
    }

    @Test
    fun addExercisePreventsDuplicateExerciseInSameWorkout() = runTest {
        val exerciseRepository = FakeExerciseRepository()
        val workoutRepository = FakeWorkoutRepository()
        val settingsRepository = FakeSettingsRepository(
            AppSettings(
                restDurationSeconds = 180,
                loadIncrementKg = 2.5,
                deloadPercent = 10,
                defaultProgressionMode = ProgressionModeCode.WEIGHT_ONLY,
            ),
        )
        val viewModel = WorkoutEditorViewModel(
            savedStateHandle = SavedStateHandle(),
            workoutRepository = workoutRepository,
            exerciseRepository = exerciseRepository,
            settingsRepository = settingsRepository,
            workoutSlotDefaultsResolver = WorkoutSlotDefaultsResolver(),
        )
        advanceUntilIdle()

        viewModel.addExercise(1001L)
        advanceUntilIdle()
        viewModel.addExercise(1001L)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.slots.size)
        assertNotNull(viewModel.uiState.value.message)
    }

    @Test
    fun updateSlotTargetsUpdatesSetsAndReps() = runTest {
        val exerciseRepository = FakeExerciseRepository()
        val workoutRepository = FakeWorkoutRepository()
        val settingsRepository = FakeSettingsRepository(
            AppSettings(
                restDurationSeconds = 180,
                loadIncrementKg = 2.5,
                deloadPercent = 10,
                defaultProgressionMode = ProgressionModeCode.WEIGHT_ONLY,
            ),
        )
        val viewModel = WorkoutEditorViewModel(
            savedStateHandle = SavedStateHandle(),
            workoutRepository = workoutRepository,
            exerciseRepository = exerciseRepository,
            settingsRepository = settingsRepository,
            workoutSlotDefaultsResolver = WorkoutSlotDefaultsResolver(),
        )
        advanceUntilIdle()

        viewModel.addExercise(1001L)
        advanceUntilIdle()
        viewModel.updateSlotTargets(position = 0, targetSets = 3, targetReps = 8)

        val slot = viewModel.uiState.value.slots.single()
        assertEquals(3, slot.targetSets)
        assertEquals(8, slot.targetReps)
        assertEquals(8, slot.repRangeMin)
        assertEquals(8, slot.repRangeMax)
    }
}

private class FakeExerciseRepository : ExerciseRepository {
    override suspend fun save(exercise: Exercise): Long = exercise.id
    override suspend fun getById(exerciseId: Long): Exercise? = getAll().firstOrNull { it.id == exerciseId }
    override suspend fun getAll(): List<Exercise> =
        listOf(
            Exercise(
                id = 1001,
                name = "Bench Press",
                primaryMuscleGroup = MuscleGroupCode.CHEST,
                secondaryMuscleGroups = emptyList(),
                equipmentType = EquipmentTypeCode.BARBELL,
            ),
        )

    override suspend fun delete(exerciseId: Long) = Unit
}

private class FakeWorkoutRepository : WorkoutRepository {
    override suspend fun createWorkout(workout: Workout): Long = 1L
    override suspend fun updateWorkout(workout: Workout) = Unit
    override suspend fun deleteWorkout(workoutId: Long) = Unit
    override suspend fun getWorkout(workoutId: Long): Workout? = null
    override suspend fun getAllWorkouts(): List<Workout> = emptyList()
}

private class FakeSettingsRepository(
    initial: AppSettings,
) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: Flow<AppSettings> = state

    override suspend fun updateDefaults(
        restDurationSeconds: Int,
        loadIncrementKg: Double,
        deloadPercent: Int,
        defaultProgressionMode: String,
    ) {
        state.value =
            state.value.copy(
                restDurationSeconds = restDurationSeconds,
                loadIncrementKg = loadIncrementKg,
                deloadPercent = deloadPercent,
                defaultProgressionMode = defaultProgressionMode,
            )
    }
}
