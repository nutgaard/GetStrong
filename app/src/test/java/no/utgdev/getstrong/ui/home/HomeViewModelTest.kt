package no.utgdev.getstrong.ui.home

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import no.utgdev.getstrong.domain.model.ActiveSessionState
import no.utgdev.getstrong.domain.model.SessionSetType
import no.utgdev.getstrong.domain.model.Exercise
import no.utgdev.getstrong.domain.model.SetResult
import no.utgdev.getstrong.domain.model.SlotProgressionUpdate
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.Workout
import no.utgdev.getstrong.domain.model.WorkoutExerciseSlot
import no.utgdev.getstrong.domain.model.WorkoutHistoryItem
import no.utgdev.getstrong.domain.model.WorkoutSession
import no.utgdev.getstrong.domain.model.WorkoutSummary
import no.utgdev.getstrong.domain.repository.ExerciseRepository
import no.utgdev.getstrong.domain.repository.SessionRepository
import no.utgdev.getstrong.domain.repository.SettingsRepository
import no.utgdev.getstrong.domain.repository.WorkoutRepository
import no.utgdev.getstrong.domain.repository.WorkoutSummaryRepository
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
    fun loadShowsEmptyStateWhenNoWorkoutsExist() = runTest {
        val workoutRepository = FakeWorkoutRepository(workouts = emptyList())
        val viewModel = createViewModel(
            workoutRepository = workoutRepository,
            exerciseRepository = FakeExerciseRepository(emptyList()),
            settingsRepository = FakeSettingsRepository(trainingDays = emptyList()),
            workoutSummaryRepository = FakeWorkoutSummaryRepository(emptyList()),
            sessionRepository = FakeSessionRepository(),
        )

        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(!state.isLoading)
        assertNull(state.errorMessage)
        assertTrue(!state.hasSavedWorkouts)
        assertTrue(state.upcomingWorkouts.isEmpty())
    }

    @Test
    fun loadBuildsSevenDayScheduleDrivenQueueAfterLastCompletedWorkout() = runTest {
        val workouts = listOf(
            workout(
                id = 1L,
                name = "Workout A",
                slots = listOf(slot(exerciseId = 1001L, position = 0), slot(exerciseId = 1002L, position = 1)),
            ),
            workout(
                id = 2L,
                name = "Workout B",
                slots = listOf(slot(exerciseId = 1003L, position = 0), slot(exerciseId = 1004L, position = 1)),
            ),
            workout(
                id = 3L,
                name = "Workout C",
                slots = listOf(slot(exerciseId = 1005L, position = 0)),
            ),
        )
        val exercises = listOf(
            exercise(1001L, "Bench Press"),
            exercise(1002L, "Barbell Row"),
            exercise(1003L, "Squat"),
            exercise(1004L, "Overhead Press"),
            exercise(1005L, "Deadlift"),
        )
        val summaries = listOf(
            WorkoutSummary(
                workoutId = 1L,
                sessionId = 50L,
                workoutName = "Workout A",
                totalVolumeKg = 500.0,
                totalDurationSeconds = 1800L,
                completedAtEpochMs = 1000L,
            ),
        )
        val viewModel = createViewModel(
            workoutRepository = FakeWorkoutRepository(workouts),
            exerciseRepository = FakeExerciseRepository(exercises),
            settingsRepository = FakeSettingsRepository(trainingDays = listOf(1, 2, 3, 4, 5, 6, 7)),
            workoutSummaryRepository = FakeWorkoutSummaryRepository(summaries),
            sessionRepository = FakeSessionRepository(),
        )

        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(!state.isLoading)
        assertEquals(7, state.upcomingWorkouts.size)
        assertEquals(listOf("Workout B", "Workout C", "Workout A"), state.upcomingWorkouts.take(3).map { it.workoutName })
        assertEquals(listOf("Squat", "Overhead Press"), state.upcomingWorkouts.first().exercisePreview)
        assertTrue(state.upcomingWorkouts.first().isNextUp)
    }

    @Test
    fun startQuickWorkoutStartsFirstUpcomingWorkout() = runTest {
        val sessionRepository = FakeSessionRepository()
        val workoutRepository = FakeWorkoutRepository(
            workouts = listOf(
                workout(
                    id = 7L,
                    name = "Workout A",
                    slots = listOf(slot(exerciseId = 1001L, position = 0)),
                ),
            ),
        )
        val exerciseRepository = FakeExerciseRepository(listOf(exercise(1001L, "Bench Press")))
        val viewModel = createViewModel(
            workoutRepository = workoutRepository,
            exerciseRepository = exerciseRepository,
            settingsRepository = FakeSettingsRepository(trainingDays = listOf(1, 2, 3, 4, 5, 6, 7)),
            workoutSummaryRepository = FakeWorkoutSummaryRepository(emptyList()),
            sessionRepository = sessionRepository,
        )

        viewModel.load()
        advanceUntilIdle()
        val sessionId = viewModel.startQuickWorkout()
        advanceUntilIdle()

        assertEquals(99L, sessionId)
        assertEquals(7L, sessionRepository.startedWorkoutId)
        assertTrue(!viewModel.uiState.value.isStartingWorkout)
        assertNull(viewModel.uiState.value.startErrorMessage)
    }

    @Test
    fun loadExposesResumeStateAndContinueCandidateWhenSessionIsUnfinished() = runTest {
        val sessionRepository = FakeSessionRepository().apply {
            unfinishedSessionId = 42L
            activeSessions[42L] = ActiveSessionState(
                session = WorkoutSession(
                    id = 42L,
                    workoutId = 2L,
                    startedAtEpochMs = 1_000L,
                    endedAtEpochMs = null,
                ),
                plannedSets = listOf(
                    SessionPlannedSet(
                        id = 1L,
                        sessionId = 42L,
                        workoutSlotId = 11L,
                        setOrder = 0,
                        exerciseId = 1003L,
                        setType = SessionSetType.WORK,
                        targetReps = 5,
                        targetWeightKg = 100.0,
                        isCompleted = true,
                        completedReps = 5,
                    ),
                ),
            )
        }
        val viewModel = createViewModel(
            workoutRepository = FakeWorkoutRepository(
                workouts = listOf(
                    workout(id = 1L, name = "Workout A", slots = listOf(slot(exerciseId = 1001L, position = 0))),
                    workout(id = 2L, name = "Workout B", slots = listOf(slot(exerciseId = 1003L, position = 0))),
                ),
            ),
            exerciseRepository = FakeExerciseRepository(listOf(exercise(1001L, "Bench"), exercise(1003L, "Squat"))),
            settingsRepository = FakeSettingsRepository(trainingDays = listOf(1, 2, 3, 4, 5, 6, 7)),
            workoutSummaryRepository = FakeWorkoutSummaryRepository(emptyList()),
            sessionRepository = sessionRepository,
        )

        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(42L, state.unfinishedSessionId)
        assertEquals("Workout B", state.upcomingWorkouts.first().workoutName)
        assertTrue(state.upcomingWorkouts.first().isResumeCandidate)
    }

    @Test
    fun startQuickWorkoutReturnsExistingUnfinishedSessionId() = runTest {
        val sessionRepository = FakeSessionRepository().apply {
            unfinishedSessionId = 42L
            activeSessions[42L] = ActiveSessionState(
                session = WorkoutSession(
                    id = 42L,
                    workoutId = 7L,
                    startedAtEpochMs = 1_000L,
                    endedAtEpochMs = null,
                ),
                plannedSets = listOf(
                    SessionPlannedSet(
                        id = 1L,
                        sessionId = 42L,
                        workoutSlotId = 11L,
                        setOrder = 0,
                        exerciseId = 1001L,
                        setType = SessionSetType.WORK,
                        targetReps = 5,
                        targetWeightKg = 100.0,
                        isCompleted = true,
                        completedReps = 5,
                    ),
                ),
            )
        }
        val viewModel = createViewModel(
            workoutRepository = FakeWorkoutRepository(
                workouts = listOf(
                    workout(
                        id = 7L,
                        name = "Workout A",
                        slots = listOf(slot(exerciseId = 1001L, position = 0)),
                    ),
                ),
            ),
            exerciseRepository = FakeExerciseRepository(listOf(exercise(1001L, "Bench Press"))),
            settingsRepository = FakeSettingsRepository(trainingDays = listOf(1, 2, 3, 4, 5, 6, 7)),
            workoutSummaryRepository = FakeWorkoutSummaryRepository(emptyList()),
            sessionRepository = sessionRepository,
        )

        viewModel.load()
        advanceUntilIdle()
        val sessionId = viewModel.startQuickWorkout()
        advanceUntilIdle()

        assertEquals(42L, sessionId)
        assertEquals(null, sessionRepository.startedWorkoutId)
    }

    @Test
    fun loadShowsErrorStateWhenWorkoutLoadFails() = runTest {
        val viewModel = createViewModel(
            workoutRepository = FakeWorkoutRepository(workouts = emptyList(), throwOnGetAll = true),
            exerciseRepository = FakeExerciseRepository(emptyList()),
            settingsRepository = FakeSettingsRepository(trainingDays = listOf(1, 3, 5)),
            workoutSummaryRepository = FakeWorkoutSummaryRepository(emptyList()),
            sessionRepository = FakeSessionRepository(),
        )

        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(!state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(state.upcomingWorkouts.isEmpty())
    }

    private fun createViewModel(
        workoutRepository: WorkoutRepository,
        exerciseRepository: ExerciseRepository,
        settingsRepository: SettingsRepository,
        workoutSummaryRepository: WorkoutSummaryRepository,
        sessionRepository: SessionRepository,
    ): HomeViewModel {
        val useCase = StartWorkoutSessionUseCase(
            workoutRepository = workoutRepository,
            exerciseRepository = exerciseRepository,
            sessionRepository = sessionRepository,
            sessionEngine = WorkoutSessionEngine(WarmupGenerator()),
        )
        return HomeViewModel(
            workoutRepository = workoutRepository,
            exerciseRepository = exerciseRepository,
            settingsRepository = settingsRepository,
            sessionRepository = sessionRepository,
            workoutSummaryRepository = workoutSummaryRepository,
            startWorkoutSessionUseCase = useCase,
        )
    }
}

private class FakeExerciseRepository(
    private val exercises: List<Exercise>,
) : ExerciseRepository {
    override suspend fun save(exercise: Exercise): Long = exercise.id

    override suspend fun getById(exerciseId: Long): Exercise? =
        exercises.firstOrNull { it.id == exerciseId }

    override suspend fun getAll(): List<Exercise> = exercises

    override suspend fun delete(exerciseId: Long) = Unit
}

private class FakeWorkoutRepository(
    private val workouts: List<Workout>,
    private val throwOnGetAll: Boolean = false,
) : WorkoutRepository {
    override suspend fun createWorkout(workout: Workout): Long = workout.id
    override suspend fun updateWorkout(workout: Workout) = Unit
    override suspend fun deleteWorkout(workoutId: Long) = Unit
    override suspend fun getWorkout(workoutId: Long): Workout? = workouts.firstOrNull { it.id == workoutId }
    override suspend fun getAllWorkouts(): List<Workout> {
        if (throwOnGetAll) throw IllegalStateException("workout load failed")
        return workouts
    }
}

private class FakeSessionRepository : SessionRepository {
    var startedWorkoutId: Long? = null
    var unfinishedSessionId: Long? = null
    val activeSessions: MutableMap<Long, ActiveSessionState> = linkedMapOf()

    override suspend fun startSession(workoutId: Long, plannedSets: List<SessionPlannedSet>): Long {
        startedWorkoutId = workoutId
        return 99L
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

private class FakeSettingsRepository(
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

    override val settings: kotlinx.coroutines.flow.Flow<no.utgdev.getstrong.domain.model.AppSettings> = state

    override suspend fun updateDefaults(
        restDurationSeconds: Int,
        loadIncrementKg: Double,
        deloadPercent: Int,
        defaultProgressionMode: String,
    ) = Unit

    override suspend fun updateTrainingDays(trainingDays: List<Int>) {
        state.value = state.value.copy(trainingDays = trainingDays)
    }
}

private class FakeWorkoutSummaryRepository(
    private val summaries: List<WorkoutSummary>,
) : WorkoutSummaryRepository {
    override suspend fun saveSummary(summary: WorkoutSummary): Long = 1L
    override suspend fun getAllSummaries(): List<WorkoutSummary> = summaries
    override suspend fun getSummaryBySessionId(sessionId: Long): WorkoutSummary? = summaries.firstOrNull { it.sessionId == sessionId }
    override suspend fun getHistory(): List<WorkoutHistoryItem> = emptyList()
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

private fun workout(
    id: Long,
    name: String,
    slots: List<WorkoutExerciseSlot>,
): Workout =
    Workout(
        id = id,
        name = name,
        slots = slots.map { it.copy(workoutId = id) },
    )

private fun slot(
    exerciseId: Long,
    position: Int,
): WorkoutExerciseSlot =
    WorkoutExerciseSlot(
        workoutId = 0L,
        exerciseId = exerciseId,
        position = position,
        targetSets = 5,
        targetReps = 5,
        repRangeMin = 5,
        repRangeMax = 5,
        progressionMode = "WEIGHT_ONLY",
        incrementKg = 2.5,
        deloadPercent = 10,
        currentWorkingWeightKg = 100.0,
        failureStreak = 0,
        lastProgressionSessionId = null,
        restSecondsOverride = null,
    )
