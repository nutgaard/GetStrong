package no.utgdev.getstrong.ui.history

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import no.utgdev.getstrong.domain.model.Exercise
import no.utgdev.getstrong.domain.model.ExerciseHistoryEntry
import no.utgdev.getstrong.domain.model.WorkoutHistoryItem
import no.utgdev.getstrong.domain.model.WorkoutSessionSummary
import no.utgdev.getstrong.domain.model.WorkoutSessionSummarySet
import no.utgdev.getstrong.domain.model.WorkoutSummary
import no.utgdev.getstrong.domain.repository.ExerciseRepository
import no.utgdev.getstrong.domain.repository.SessionSummaryRepository
import no.utgdev.getstrong.domain.repository.WorkoutSummaryRepository
import no.utgdev.getstrong.ui.navigation.AppDestination
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadShowsEmptyStateWhenNoHistoryExists() = runTest {
        val viewModel = HistoryViewModel(
            workoutSummaryRepository = FakeWorkoutSummaryRepository(historyItems = emptyList()),
            sessionSummaryRepository = FakeSessionSummaryRepository(),
            exerciseRepository = FakeExerciseRepository(),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(!state.isLoading)
        assertNull(state.errorMessage)
        assertTrue(state.workouts.isEmpty())
    }

    @Test
    fun loadShowsErrorStateWhenHistoryReadFails() = runTest {
        val viewModel = HistoryViewModel(
            workoutSummaryRepository = FakeWorkoutSummaryRepository(historyItems = emptyList(), throwOnHistory = true),
            sessionSummaryRepository = FakeSessionSummaryRepository(),
            exerciseRepository = FakeExerciseRepository(),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(!state.isLoading)
        assertNotNull(state.errorMessage)
        assertEquals(0, state.workouts.size)
    }

    @Test
    fun loadBuildsWorkoutCardsWithExerciseRows() = runTest {
        val summary = WorkoutSessionSummary(
            sessionId = 77L,
            workoutId = 10L,
            totalVolumeKg = 500.0,
            totalDurationSeconds = 1800L,
            volumeRule = "WORK_SETS_ONLY",
            sets = listOf(
                WorkoutSessionSummarySet(0, "WARMUP", 1001L, 3, 3, 60.0),
                WorkoutSessionSummarySet(1, "WORK", 1001L, 5, 5, 100.0),
                WorkoutSessionSummarySet(2, "WORK", 1001L, 5, 4, 100.0),
            ),
        )
        val viewModel = HistoryViewModel(
            workoutSummaryRepository = FakeWorkoutSummaryRepository(
                historyItems = listOf(
                    WorkoutHistoryItem(
                        id = 1L,
                        sessionId = 77L,
                        workoutName = "A Workout",
                        totalVolumeKg = 500.0,
                        totalDurationSeconds = 1800L,
                        completedAtEpochMs = 1_000L,
                    ),
                ),
            ),
            sessionSummaryRepository = FakeSessionSummaryRepository(
                summaries = mapOf(77L to summary),
            ),
            exerciseRepository = FakeExerciseRepository(),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.workouts.size)
        assertEquals("A Workout", state.workouts.single().workoutName)
        assertEquals(1, state.workouts.single().exerciseResults.size)
        assertEquals("Bench Press", state.workouts.single().exerciseResults.single().exerciseName)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseHistoryViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadMapsExerciseHistoryRows() = runTest {
        val viewModel = ExerciseHistoryViewModel(
            savedStateHandle = SavedStateHandle(mapOf(AppDestination.ExerciseHistory.EXERCISE_ID_ARG to "1001")),
            sessionSummaryRepository = FakeSessionSummaryRepository(
                exerciseHistory = mapOf(
                    1001L to listOf(
                        ExerciseHistoryEntry(
                            exerciseId = 1001L,
                            sessionId = 77L,
                            workoutName = "A Workout",
                            completedAtEpochMs = 1_000L,
                            reps = 5,
                            weightKg = 100.0,
                            estimatedOneRepMaxKg = 116.7,
                        ),
                    ),
                ),
            ),
            exerciseRepository = FakeExerciseRepository(),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(!state.isLoading)
        assertEquals("Bench Press", state.exerciseName)
        assertEquals(1, state.rows.size)
        assertEquals(116.7, state.rows.single().estimatedOneRepMaxKg, 0.0)
    }
}

private class FakeWorkoutSummaryRepository(
    private val historyItems: List<WorkoutHistoryItem>,
    private val throwOnHistory: Boolean = false,
) : WorkoutSummaryRepository {
    override suspend fun saveSummary(summary: WorkoutSummary): Long = 1L
    override suspend fun getAllSummaries(): List<WorkoutSummary> = emptyList()
    override suspend fun getSummaryBySessionId(sessionId: Long): WorkoutSummary? = null
    override suspend fun getHistory(): List<WorkoutHistoryItem> {
        if (throwOnHistory) throw IllegalStateException("history load failed")
        return historyItems
    }
}

private class FakeSessionSummaryRepository(
    private val summaries: Map<Long, WorkoutSessionSummary> = emptyMap(),
    private val exerciseHistory: Map<Long, List<ExerciseHistoryEntry>> = emptyMap(),
) : SessionSummaryRepository {
    override suspend fun getSessionSummary(sessionId: Long): WorkoutSessionSummary? = summaries[sessionId]

    override suspend fun getExerciseHistory(exerciseId: Long): List<ExerciseHistoryEntry> =
        exerciseHistory[exerciseId].orEmpty()
}

private class FakeExerciseRepository : ExerciseRepository {
    override suspend fun save(exercise: Exercise): Long = exercise.id

    override suspend fun getById(exerciseId: Long): Exercise? =
        Exercise(
            id = exerciseId,
            name = "Bench Press",
            primaryMuscleGroup = "CHEST",
            secondaryMuscleGroups = emptyList(),
            equipmentType = "BARBELL",
        )

    override suspend fun getAll(): List<Exercise> = emptyList()

    override suspend fun delete(exerciseId: Long) = Unit
}
