package no.utgdev.getstrong.ui.progress

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
import no.utgdev.getstrong.domain.model.WorkoutSessionSummary
import no.utgdev.getstrong.domain.repository.ExerciseRepository
import no.utgdev.getstrong.domain.repository.SessionSummaryRepository
import no.utgdev.getstrong.ui.navigation.AppDestination
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadBuildsTrackedExerciseOverviewRows() = runTest {
        val benchEntries = listOf(
            exerciseHistoryEntry(
                exerciseId = 1001L,
                sessionId = 7L,
                date = "2026-01-10",
                reps = 5,
                weightKg = 100.0,
                estimatedOneRepMaxKg = 116.7,
            ),
            exerciseHistoryEntry(
                exerciseId = 1001L,
                sessionId = 8L,
                date = "2026-02-10",
                reps = 5,
                weightKg = 102.5,
                estimatedOneRepMaxKg = 119.6,
            ),
            exerciseHistoryEntry(
                exerciseId = 1001L,
                sessionId = 8L,
                date = "2026-02-10",
                reps = 4,
                weightKg = 100.0,
                estimatedOneRepMaxKg = 113.3,
            ),
        )
        val rowEntries = listOf(
            exerciseHistoryEntry(
                exerciseId = 1002L,
                sessionId = 9L,
                date = "2026-02-20",
                reps = 8,
                weightKg = 40.0,
                estimatedOneRepMaxKg = 50.7,
            ),
        )
        val viewModel = ProgressViewModel(
            sessionSummaryRepository = FakeSessionSummaryRepository(
                allExerciseHistory = benchEntries + rowEntries,
            ),
            exerciseRepository = FakeExerciseRepository(
                exercises = listOf(
                    exercise(id = 1001L, name = "Bench Press"),
                    exercise(id = 1002L, name = "Barbell Row"),
                ),
            ),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(!state.isLoading)
        assertEquals(2, state.exercises.size)
        assertEquals("Barbell Row", state.exercises.first().exerciseName)

        val bench = state.exercises.single { it.exerciseId == 1001L }
        assertEquals(102.5, bench.latestWeightKg, 0.0)
        assertEquals(5, bench.latestReps)
        assertEquals(listOf(116.7, 119.6), bench.trendPoints)
    }

    @Test
    fun loadShowsErrorWhenOverviewReadFails() = runTest {
        val viewModel = ProgressViewModel(
            sessionSummaryRepository = FakeSessionSummaryRepository(throwOnAllExerciseHistory = true),
            exerciseRepository = FakeExerciseRepository(),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(!state.isLoading)
        assertNotNull(state.errorMessage)
        assertEquals(0, state.exercises.size)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseProgressViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadDefaultsToThreeMonthsAndCanExpandToAllHistory() = runTest {
        val viewModel = ExerciseProgressViewModel(
            savedStateHandle = SavedStateHandle(mapOf(AppDestination.ExerciseProgress.EXERCISE_ID_ARG to "1001")),
            sessionSummaryRepository = FakeSessionSummaryRepository(
                exerciseHistory = mapOf(
                    1001L to listOf(
                        exerciseHistoryEntry(
                            exerciseId = 1001L,
                            sessionId = 1L,
                            date = "2025-09-10",
                            reps = 5,
                            weightKg = 90.0,
                            estimatedOneRepMaxKg = 105.0,
                        ),
                        exerciseHistoryEntry(
                            exerciseId = 1001L,
                            sessionId = 2L,
                            date = "2025-12-15",
                            reps = 5,
                            weightKg = 100.0,
                            estimatedOneRepMaxKg = 116.7,
                        ),
                        exerciseHistoryEntry(
                            exerciseId = 1001L,
                            sessionId = 3L,
                            date = "2026-02-10",
                            reps = 5,
                            weightKg = 102.5,
                            estimatedOneRepMaxKg = 119.6,
                        ),
                    ),
                ),
            ),
            exerciseRepository = FakeExerciseRepository(
                exercises = listOf(exercise(id = 1001L, name = "Bench Press")),
            ),
        )
        advanceUntilIdle()

        val initial = viewModel.uiState.value
        assertEquals(ProgressRangeOption.THREE_MONTHS, initial.selectedRange)
        assertEquals(2, initial.points.size)
        assertEquals(102.5, initial.latestWeightKg, 0.0)
        assertEquals(119.6, initial.bestEstimatedOneRepMaxKg, 0.0)

        viewModel.selectRange(ProgressRangeOption.ALL)

        val all = viewModel.uiState.value
        assertEquals(3, all.points.size)
        assertEquals(3, all.totalSessions)
    }
}

private class FakeSessionSummaryRepository(
    private val exerciseHistory: Map<Long, List<ExerciseHistoryEntry>> = emptyMap(),
    private val allExerciseHistory: List<ExerciseHistoryEntry> = emptyList(),
    private val throwOnAllExerciseHistory: Boolean = false,
) : SessionSummaryRepository {
    override suspend fun getSessionSummary(sessionId: Long): WorkoutSessionSummary? = null

    override suspend fun getExerciseHistory(exerciseId: Long): List<ExerciseHistoryEntry> =
        exerciseHistory[exerciseId].orEmpty()

    override suspend fun getAllExerciseHistory(): List<ExerciseHistoryEntry> {
        if (throwOnAllExerciseHistory) throw IllegalStateException("progress load failed")
        return allExerciseHistory
    }
}

private class FakeExerciseRepository(
    private val exercises: List<Exercise> = emptyList(),
) : ExerciseRepository {
    override suspend fun save(exercise: Exercise): Long = exercise.id

    override suspend fun getById(exerciseId: Long): Exercise? =
        exercises.firstOrNull { it.id == exerciseId }

    override suspend fun getAll(): List<Exercise> = exercises

    override suspend fun delete(exerciseId: Long) = Unit
}

private fun exercise(
    id: Long,
    name: String,
): Exercise =
    Exercise(
        id = id,
        name = name,
        primaryMuscleGroup = "CHEST",
        secondaryMuscleGroups = emptyList(),
        equipmentType = "BARBELL",
    )

private fun exerciseHistoryEntry(
    exerciseId: Long,
    sessionId: Long,
    date: String,
    reps: Int,
    weightKg: Double,
    estimatedOneRepMaxKg: Double,
): ExerciseHistoryEntry =
    ExerciseHistoryEntry(
        exerciseId = exerciseId,
        sessionId = sessionId,
        workoutName = "Workout $sessionId",
        completedAtEpochMs = LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        reps = reps,
        weightKg = weightKg,
        estimatedOneRepMaxKg = estimatedOneRepMaxKg,
    )
