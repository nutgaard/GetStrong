package no.utgdev.getstrong.ui.planning

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import no.utgdev.getstrong.domain.model.ProgressionModeCode
import no.utgdev.getstrong.domain.model.ActiveSessionState
import no.utgdev.getstrong.domain.model.Exercise
import no.utgdev.getstrong.domain.model.SetResult
import no.utgdev.getstrong.domain.model.SlotProgressionUpdate
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.Workout
import no.utgdev.getstrong.domain.model.WorkoutExerciseSlot
import no.utgdev.getstrong.domain.model.WorkoutSession
import no.utgdev.getstrong.domain.repository.ExerciseRepository
import no.utgdev.getstrong.domain.repository.SessionRepository
import no.utgdev.getstrong.domain.repository.SettingsRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
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

    @Test
    fun startWorkoutSessionStartsChosenWorkout() = runTest {
        val workoutId = 7L
        val sessionRepository = PlanningFakeSessionRepository()
        val viewModel = createViewModel(
            workoutRepository = PlanningFakeWorkoutRepository(
                returned = listOf(
                    Workout(
                        id = workoutId,
                        name = "Workout A",
                        slots = listOf(
                            WorkoutExerciseSlot(
                                id = 11L,
                                workoutId = workoutId,
                                exerciseId = 1006L,
                                position = 0,
                                targetSets = 2,
                                targetReps = 5,
                                repRangeMin = 5,
                                repRangeMax = 5,
                                progressionMode = ProgressionModeCode.WEIGHT_ONLY,
                                incrementKg = 2.5,
                                deloadPercent = 10,
                                currentWorkingWeightKg = 100.0,
                                failureStreak = 0,
                                restSecondsOverride = null,
                            ),
                        ),
                    ),
                ),
            ),
            sessionRepository = sessionRepository,
        )
        advanceUntilIdle()

        val sessionId = viewModel.startWorkoutSession(workoutId)

        assertEquals(10L, sessionId)
        assertEquals(workoutId, sessionRepository.startedWorkoutId)
    }

    @Test
    fun startWorkoutSessionReusesExistingUnfinishedSession() = runTest {
        val workoutId = 7L
        val sessionRepository = PlanningFakeSessionRepository().apply {
            unfinishedSessionId = 55L
            activeSessions[55L] = ActiveSessionState(
                session = WorkoutSession(
                    id = 55L,
                    workoutId = workoutId,
                    startedAtEpochMs = 1_000L,
                    endedAtEpochMs = null,
                ),
                plannedSets = emptyList(),
            )
        }
        val viewModel = createViewModel(
            workoutRepository = PlanningFakeWorkoutRepository(
                returned = listOf(
                    Workout(
                        id = workoutId,
                        name = "Workout A",
                        slots = listOf(
                            WorkoutExerciseSlot(
                                id = 11L,
                                workoutId = workoutId,
                                exerciseId = 1006L,
                                position = 0,
                                targetSets = 2,
                                targetReps = 5,
                                repRangeMin = 5,
                                repRangeMax = 5,
                                progressionMode = ProgressionModeCode.WEIGHT_ONLY,
                                incrementKg = 2.5,
                                deloadPercent = 10,
                                currentWorkingWeightKg = 100.0,
                                failureStreak = 0,
                                restSecondsOverride = null,
                            ),
                        ),
                    ),
                ),
            ),
            sessionRepository = sessionRepository,
        )
        advanceUntilIdle()

        val sessionId = viewModel.startWorkoutSession(workoutId)

        assertEquals(55L, sessionId)
        assertEquals(null, sessionRepository.startedWorkoutId)
        assertEquals(55L, viewModel.uiState.value.unfinishedSessionId)
        assertEquals(workoutId, viewModel.uiState.value.unfinishedSessionWorkoutId)
    }

    @Test
    fun toggleTrainingDayPersistsSelection() = runTest {
        val settingsRepository = PlanningFakeSettingsRepository(trainingDays = listOf(1, 3, 5))
        val viewModel = createViewModel(
            workoutRepository = PlanningFakeWorkoutRepository(returned = emptyList()),
            settingsRepository = settingsRepository,
        )
        advanceUntilIdle()

        viewModel.toggleTrainingDay(2)
        advanceUntilIdle()

        assertEquals(listOf(1, 2, 3, 5), viewModel.uiState.value.trainingDays)
        assertEquals(listOf(1, 2, 3, 5), settingsRepository.savedTrainingDays)
    }

    private fun createViewModel(
        workoutRepository: WorkoutRepository,
        settingsRepository: SettingsRepository = PlanningFakeSettingsRepository(trainingDays = listOf(1, 3, 5)),
        sessionRepository: PlanningFakeSessionRepository = PlanningFakeSessionRepository(),
    ): PlanningViewModel {
        val useCase = StartWorkoutSessionUseCase(
            workoutRepository = workoutRepository,
            exerciseRepository = PlanningFakeExerciseRepository(),
            sessionRepository = sessionRepository,
            sessionEngine = WorkoutSessionEngine(WarmupGenerator()),
        )
        return PlanningViewModel(
            workoutRepository = workoutRepository,
            settingsRepository = settingsRepository,
            sessionRepository = sessionRepository,
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
    var startedWorkoutId: Long? = null
    var unfinishedSessionId: Long? = null
    val activeSessions: MutableMap<Long, ActiveSessionState> = linkedMapOf()

    override suspend fun startSession(workoutId: Long, plannedSets: List<SessionPlannedSet>): Long {
        startedWorkoutId = workoutId
        return 10L
    }
    override suspend fun findUnfinishedSessionId(): Long? = unfinishedSessionId
    override suspend fun discardSessionIfNoProgress(sessionId: Long): Boolean = false
    override suspend fun getActiveSessionState(sessionId: Long): ActiveSessionState? = activeSessions[sessionId]
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

private class PlanningFakeSettingsRepository(
    trainingDays: List<Int>,
) : SettingsRepository {
    private val state = kotlinx.coroutines.flow.MutableStateFlow(
        no.utgdev.getstrong.domain.model.AppSettings(
            restDurationSeconds = 180,
            loadIncrementKg = 2.5,
            deloadPercent = 10,
            defaultProgressionMode = "WEIGHT_ONLY",
            trainingDays = trainingDays,
        ),
    )
    var savedTrainingDays: List<Int> = trainingDays

    override val settings: kotlinx.coroutines.flow.Flow<no.utgdev.getstrong.domain.model.AppSettings> = state

    override suspend fun updateDefaults(
        restDurationSeconds: Int,
        loadIncrementKg: Double,
        deloadPercent: Int,
        defaultProgressionMode: String,
    ) = Unit

    override suspend fun updateTrainingDays(trainingDays: List<Int>) {
        savedTrainingDays = trainingDays
        state.value = state.value.copy(trainingDays = trainingDays)
    }
}
