package no.utgdev.getstrong.ui.home

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import no.utgdev.getstrong.domain.model.ActiveSessionState
import no.utgdev.getstrong.domain.model.AppSettings
import no.utgdev.getstrong.domain.model.Exercise
import no.utgdev.getstrong.domain.model.SetResult
import no.utgdev.getstrong.domain.model.SlotProgressionUpdate
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.Workout
import no.utgdev.getstrong.domain.model.WorkoutHistoryItem
import no.utgdev.getstrong.domain.model.WorkoutSession
import no.utgdev.getstrong.domain.model.WorkoutSummary
import no.utgdev.getstrong.domain.repository.ExerciseRepository
import no.utgdev.getstrong.domain.repository.SessionRepository
import no.utgdev.getstrong.domain.repository.SettingsRepository
import no.utgdev.getstrong.domain.repository.WorkoutRepository
import no.utgdev.getstrong.domain.repository.WorkoutSummaryRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomeViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadCatalogShowsEmptyStateWhenRepositoryReturnsNoExercises() = runTest {
        val exerciseRepository = FakeExerciseRepository(exercises = emptyList())
        val viewModel = createViewModel(exerciseRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(!state.isLoadingCatalog)
        assertNull(state.catalogErrorMessage)
        assertEquals(0, state.catalogCount)
        assertTrue(state.catalogPreview.isEmpty())
    }

    @Test
    fun loadCatalogShowsErrorStateWhenRepositoryThrows() = runTest {
        val exerciseRepository = FakeExerciseRepository(exercises = emptyList(), throwOnGetAll = true)
        val viewModel = createViewModel(exerciseRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(!state.isLoadingCatalog)
        assertNotNull(state.catalogErrorMessage)
        assertEquals(0, state.catalogCount)
    }

    private fun createViewModel(exerciseRepository: ExerciseRepository): HomeViewModel =
        HomeViewModel(
            exerciseRepository = exerciseRepository,
            workoutRepository = FakeWorkoutRepository(),
            sessionRepository = FakeSessionRepository(),
            workoutSummaryRepository = FakeWorkoutSummaryRepository(),
            settingsRepository = FakeSettingsRepository(),
        )
}

private class FakeExerciseRepository(
    private val exercises: List<Exercise>,
    private val throwOnGetAll: Boolean = false,
) : ExerciseRepository {
    override suspend fun save(exercise: Exercise): Long = exercise.id
    override suspend fun getById(exerciseId: Long): Exercise? = exercises.firstOrNull { it.id == exerciseId }
    override suspend fun getAll(): List<Exercise> {
        if (throwOnGetAll) throw IllegalStateException("catalog load failed")
        return exercises
    }
    override suspend fun delete(exerciseId: Long) = Unit
}

private class FakeWorkoutRepository : WorkoutRepository {
    override suspend fun createWorkout(workout: Workout): Long = 1L
    override suspend fun updateWorkout(workout: Workout) = Unit
    override suspend fun deleteWorkout(workoutId: Long) = Unit
    override suspend fun getWorkout(workoutId: Long): Workout? = null
    override suspend fun getAllWorkouts(): List<Workout> = emptyList()
}

private class FakeSessionRepository : SessionRepository {
    override suspend fun startSession(workoutId: Long, plannedSets: List<SessionPlannedSet>): Long = 1L
    override suspend fun getActiveSessionState(sessionId: Long): ActiveSessionState? = null
    override suspend fun completePlannedSet(sessionId: Long, plannedSetId: Long, repsAchieved: Int): ActiveSessionState? = null
    override suspend fun updatePlannedSetWeight(sessionId: Long, plannedSetId: Long, weightKg: Double): ActiveSessionState? = null
    override suspend fun addExtraSet(sessionId: Long, anchorPlannedSetId: Long): ActiveSessionState? = null
    override suspend fun removeExtraSet(sessionId: Long, plannedSetId: Long): ActiveSessionState? = null
    override suspend fun completeSession(sessionId: Long) = Unit
    override suspend fun completeSessionWithProgression(sessionId: Long, updates: List<SlotProgressionUpdate>) = Unit
    override suspend fun saveSession(session: WorkoutSession): Long = 1L
    override suspend fun saveSetResult(result: SetResult): Long = 1L
    override suspend fun getSetResults(sessionId: Long): List<SetResult> = emptyList()
}

private class FakeWorkoutSummaryRepository : WorkoutSummaryRepository {
    override suspend fun saveSummary(summary: WorkoutSummary): Long = 1L
    override suspend fun getAllSummaries(): List<WorkoutSummary> = emptyList()
    override suspend fun getSummaryBySessionId(sessionId: Long): WorkoutSummary? = null
    override suspend fun getHistory(): List<WorkoutHistoryItem> = emptyList()
}

private class FakeSettingsRepository : SettingsRepository {
    private val state = MutableStateFlow(
        AppSettings(
            restDurationSeconds = 180,
            loadIncrementKg = 2.5,
            deloadPercent = 10,
            defaultProgressionMode = "weight_only",
        ),
    )

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
