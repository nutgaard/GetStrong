package no.utgdev.getstrong.ui.planning

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import no.utgdev.getstrong.domain.model.ActiveSessionState
import no.utgdev.getstrong.domain.model.Exercise
import no.utgdev.getstrong.domain.model.SetResult
import no.utgdev.getstrong.domain.model.SlotProgressionUpdate
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.Workout
import no.utgdev.getstrong.domain.repository.ExerciseRepository
import no.utgdev.getstrong.domain.repository.SessionRepository
import no.utgdev.getstrong.domain.repository.WorkoutRepository
import no.utgdev.getstrong.domain.session.WorkoutSessionEngine
import no.utgdev.getstrong.domain.usecase.StartWorkoutSessionUseCase
import no.utgdev.getstrong.domain.usecase.WarmupGenerator
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlanningViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun refreshShowsEmptyStateWhenNoWorkoutsExist() = runTest {
        val workoutRepository = PlanningFakeWorkoutRepository(returned = emptyList())
        val viewModel = createViewModel(workoutRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(!state.isLoading)
        assertNull(state.errorMessage)
        assertTrue(state.workouts.isEmpty())
    }

    @Test
    fun refreshShowsErrorStateWhenRepositoryFails() = runTest {
        val workoutRepository = PlanningFakeWorkoutRepository(returned = emptyList(), throwOnGetAll = true)
        val viewModel = createViewModel(workoutRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(!state.isLoading)
        assertNotNull(state.errorMessage)
        assertEquals(0, state.workouts.size)
    }

    private fun createViewModel(workoutRepository: WorkoutRepository): PlanningViewModel {
        val useCase = StartWorkoutSessionUseCase(
            workoutRepository = workoutRepository,
            exerciseRepository = PlanningFakeExerciseRepository(),
            sessionRepository = PlanningFakeSessionRepository(),
            sessionEngine = WorkoutSessionEngine(WarmupGenerator()),
        )
        return PlanningViewModel(
            workoutRepository = workoutRepository,
            startWorkoutSessionUseCase = useCase,
        )
    }
}

private class PlanningFakeWorkoutRepository(
    private val returned: List<Workout>,
    private val throwOnGetAll: Boolean = false,
) : WorkoutRepository {
    override suspend fun createWorkout(workout: Workout): Long = 1L
    override suspend fun updateWorkout(workout: Workout) = Unit
    override suspend fun deleteWorkout(workoutId: Long) = Unit
    override suspend fun getWorkout(workoutId: Long): Workout? = returned.firstOrNull { it.id == workoutId }
    override suspend fun getAllWorkouts(): List<Workout> {
        if (throwOnGetAll) throw IllegalStateException("workout load failed")
        return returned
    }
}

private class PlanningFakeExerciseRepository : ExerciseRepository {
    override suspend fun save(exercise: Exercise): Long = exercise.id
    override suspend fun getById(exerciseId: Long): Exercise? = null
    override suspend fun getAll(): List<Exercise> = emptyList()
    override suspend fun delete(exerciseId: Long) = Unit
}

private class PlanningFakeSessionRepository : SessionRepository {
    override suspend fun startSession(workoutId: Long, plannedSets: List<SessionPlannedSet>): Long = 10L
    override suspend fun getActiveSessionState(sessionId: Long): ActiveSessionState? = null
    override suspend fun completePlannedSet(sessionId: Long, plannedSetId: Long, repsAchieved: Int): ActiveSessionState? = null
    override suspend fun completeSession(sessionId: Long) = Unit
    override suspend fun completeSessionWithProgression(sessionId: Long, updates: List<SlotProgressionUpdate>) = Unit
    override suspend fun saveSession(session: no.utgdev.getstrong.domain.model.WorkoutSession): Long = 1L
    override suspend fun saveSetResult(result: SetResult): Long = 1L
    override suspend fun getSetResults(sessionId: Long): List<SetResult> = emptyList()
}
