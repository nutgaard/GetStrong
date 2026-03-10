package no.utgdev.getstrong.ui.summary

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import no.utgdev.getstrong.domain.model.ExerciseHistoryEntry
import no.utgdev.getstrong.domain.model.Exercise
import no.utgdev.getstrong.domain.model.WorkoutSessionSummary
import no.utgdev.getstrong.domain.model.WorkoutSessionSummarySet
import no.utgdev.getstrong.domain.repository.ExerciseRepository
import no.utgdev.getstrong.domain.repository.SessionSummaryRepository
import no.utgdev.getstrong.ui.navigation.AppDestination
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadSummaryShowsEmptyErrorWhenRepositoryReturnsNull() = runTest {
        val viewModel = SummaryViewModel(
            savedStateHandle = SavedStateHandle(mapOf(AppDestination.Summary.SESSION_ID_ARG to "99")),
            sessionSummaryRepository = FakeSessionSummaryRepository(summary = null),
            exerciseRepository = FakeExerciseRepository(),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(!state.isLoading)
        assertEquals("Summary unavailable for this session.", state.errorMessage)
        assertTrue(state.sets.isEmpty())
    }

    @Test
    fun loadSummaryShowsErrorWhenRepositoryThrows() = runTest {
        val viewModel = SummaryViewModel(
            savedStateHandle = SavedStateHandle(mapOf(AppDestination.Summary.SESSION_ID_ARG to "99")),
            sessionSummaryRepository = FakeSessionSummaryRepository(summary = null, throwOnLoad = true),
            exerciseRepository = FakeExerciseRepository(),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(!state.isLoading)
        assertNotNull(state.errorMessage)
        assertEquals(0, state.sets.size)
    }

    @Test
    fun loadSummaryPopulatesContentWhenAvailable() = runTest {
        val summary =
            WorkoutSessionSummary(
                sessionId = 99,
                workoutId = 10,
                totalVolumeKg = 1000.0,
                totalDurationSeconds = 1800,
                volumeRule = "work-only",
                sets = listOf(WorkoutSessionSummarySet(0, "WORK", 1001, 5, 5, 100.0)),
            )
        val viewModel = SummaryViewModel(
            savedStateHandle = SavedStateHandle(mapOf(AppDestination.Summary.SESSION_ID_ARG to "99")),
            sessionSummaryRepository = FakeSessionSummaryRepository(summary = summary),
            exerciseRepository = FakeExerciseRepository(),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(!state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(1, state.sets.size)
        assertEquals(1000.0, state.totalVolumeKg, 0.0)
        assertEquals("Bench Press", state.sets.single().exerciseName)
    }
}

private class FakeSessionSummaryRepository(
    private val summary: WorkoutSessionSummary?,
    private val throwOnLoad: Boolean = false,
) : SessionSummaryRepository {
    override suspend fun getSessionSummary(sessionId: Long): WorkoutSessionSummary? {
        if (throwOnLoad) throw IllegalStateException("summary load failed")
        return summary
    }

    override suspend fun getExerciseHistory(exerciseId: Long): List<ExerciseHistoryEntry> = emptyList()

    override suspend fun getAllExerciseHistory(): List<ExerciseHistoryEntry> = emptyList()
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
