package no.utgdev.getstrong.ui.activeWorkout

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import no.utgdev.getstrong.domain.model.ActiveSessionState
import no.utgdev.getstrong.domain.model.AppSettings
import no.utgdev.getstrong.domain.model.Exercise
import no.utgdev.getstrong.domain.model.ExerciseHistoryEntry
import no.utgdev.getstrong.domain.model.ProgressionModeCode
import no.utgdev.getstrong.domain.model.SetResult
import no.utgdev.getstrong.domain.model.SlotProgressionUpdate
import no.utgdev.getstrong.domain.model.Workout
import no.utgdev.getstrong.domain.model.WorkoutHistoryItem
import no.utgdev.getstrong.domain.model.WorkoutExerciseSlot
import no.utgdev.getstrong.domain.model.WorkoutSession
import no.utgdev.getstrong.domain.model.WorkoutSummary
import no.utgdev.getstrong.domain.model.WorkoutSessionSummary
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.SessionSetType
import no.utgdev.getstrong.domain.repository.ExerciseRepository
import no.utgdev.getstrong.domain.repository.SessionRepository
import no.utgdev.getstrong.domain.repository.SessionSummaryRepository
import no.utgdev.getstrong.domain.repository.SettingsRepository
import no.utgdev.getstrong.domain.repository.WorkoutRepository
import no.utgdev.getstrong.domain.repository.WorkoutSummaryRepository
import no.utgdev.getstrong.domain.time.TimeProvider
import no.utgdev.getstrong.domain.usecase.CompleteSessionAndSaveSummaryUseCase
import no.utgdev.getstrong.domain.usecase.CompleteSessionWithProgressionUseCase
import no.utgdev.getstrong.domain.usecase.ElapsedTimeCalculator
import no.utgdev.getstrong.domain.usecase.ProgressionCalculator
import no.utgdev.getstrong.domain.usecase.RestTimerCalculator
import no.utgdev.getstrong.domain.usecase.RestTimerPolicy
import no.utgdev.getstrong.ui.navigation.AppDestination
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveWorkoutViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun tappingIncompleteSetCompletesToTargetAndStartsRestOverlay() = runTest {
        val timeProvider = MutableTimeProvider(nowMs = 1_000L)
        val sessionRepository = ActiveWorkoutFakeSessionRepository(
            activeState = activeState(
                plannedSets = listOf(
                    plannedSet(id = 1, workoutSlotId = 11, setOrder = 0, exerciseId = 1006, setType = SessionSetType.WORK, targetReps = 5),
                    plannedSet(id = 2, workoutSlotId = 11, setOrder = 1, exerciseId = 1006, setType = SessionSetType.WORK, targetReps = 5),
                ),
            ),
        )
        val viewModel = createViewModel(sessionRepository = sessionRepository, timeProvider = timeProvider)

        runCurrent()
        viewModel.onSetTapped(1)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(5, state.plannedSets.first { it.id == 1L }.completedReps)
        assertEquals(2L, state.currentSet?.id)
        assertTrue(state.isRestTimerActive)
        assertEquals(180, state.restRemainingSeconds)

        timeProvider.nowMs = 181_000L
        advanceTimeBy(181_000L)
        runCurrent()
    }

    @Test
    fun tappingCompletedSetDecrementsAndClearsBackToZero() = runTest {
        val sessionRepository = ActiveWorkoutFakeSessionRepository(
            activeState = activeState(
                plannedSets = listOf(
                    plannedSet(
                        id = 1,
                        workoutSlotId = 11,
                        setOrder = 0,
                        exerciseId = 1006,
                        setType = SessionSetType.WORK,
                        targetReps = 5,
                        completedReps = 2,
                        isCompleted = true,
                    ),
                    plannedSet(id = 2, workoutSlotId = 11, setOrder = 1, exerciseId = 1006, setType = SessionSetType.WORK, targetReps = 5),
                ),
            ),
        )
        val viewModel = createViewModel(sessionRepository = sessionRepository)

        runCurrent()
        viewModel.onSetTapped(1)
        runCurrent()
        assertEquals(1, viewModel.uiState.value.plannedSets.first { it.id == 1L }.completedReps)
        assertEquals(2L, viewModel.uiState.value.currentSet?.id)

        viewModel.onSetTapped(1)
        runCurrent()

        val state = viewModel.uiState.value
        val reopened = state.plannedSets.first { it.id == 1L }
        assertFalse(reopened.isCompleted)
        assertEquals(null, reopened.completedReps)
        assertEquals(1L, state.currentSet?.id)
    }

    @Test
    fun weightAndExtraSetChangesRefreshUiState() = runTest {
        val sessionRepository = ActiveWorkoutFakeSessionRepository(
            activeState = activeState(
                plannedSets = listOf(
                    plannedSet(id = 1, workoutSlotId = 11, setOrder = 0, exerciseId = 1006, setType = SessionSetType.WORK, targetReps = 5, targetWeightKg = 100.0),
                    plannedSet(id = 2, workoutSlotId = 11, setOrder = 1, exerciseId = 1006, setType = SessionSetType.WORK, targetReps = 5, targetWeightKg = 100.0),
                ),
            ),
        )
        val viewModel = createViewModel(sessionRepository = sessionRepository)

        runCurrent()
        viewModel.updateSetWeight(1, 102.5)
        runCurrent()
        assertEquals(102.5, viewModel.uiState.value.plannedSets.first { it.id == 1L }.targetWeightKg ?: 0.0, 0.0)

        viewModel.addExtraSet(2)
        runCurrent()
        val withExtra = viewModel.uiState.value.plannedSets
        assertEquals(3, withExtra.size)
        assertTrue(withExtra.last().isExtra)

        viewModel.removeExtraSet(withExtra.last().id)
        runCurrent()
        assertEquals(2, viewModel.uiState.value.plannedSets.size)
        assertTrue(viewModel.uiState.value.plannedSets.none { it.isExtra })
    }

    @Test
    fun finishSessionReturnsNullUntilAllPlannedSetsAreComplete() = runTest {
        val sessionRepository = ActiveWorkoutFakeSessionRepository(
            activeState = activeState(
                plannedSets = listOf(
                    plannedSet(id = 1, workoutSlotId = 11, setOrder = 0, exerciseId = 1006, setType = SessionSetType.WORK, targetReps = 5),
                ),
            ),
        )
        val viewModel = createViewModel(sessionRepository = sessionRepository)

        runCurrent()
        val result = viewModel.finishSession()
        runCurrent()

        assertNull(result)
        assertEquals(0, sessionRepository.completeWithProgressionCalls)
        assertFalse(viewModel.uiState.value.isCompleted)
    }

    @Test
    fun finishSessionCompletesAndSavesSummaryWhenAllPlannedSetsAreHandled() = runTest {
        val sessionRepository = ActiveWorkoutFakeSessionRepository(
            activeState = activeState(
                plannedSets = listOf(
                    plannedSet(
                        id = 1,
                        workoutSlotId = 11,
                        setOrder = 0,
                        exerciseId = 1006,
                        setType = SessionSetType.WORK,
                        targetReps = 5,
                        targetWeightKg = 100.0,
                        completedReps = 5,
                        isCompleted = true,
                    ),
                ),
            ),
            setResults = listOf(
                SetResult(
                    id = 1,
                    sessionId = 42L,
                    plannedSetId = 1L,
                    workoutSlotId = 11L,
                    exerciseId = 1006L,
                    setType = SessionSetType.WORK,
                    reps = 5,
                    weightKg = 100.0,
                ),
            ),
        )
        val workoutRepository = FakeWorkoutRepository(
            workout = Workout(
                id = 5L,
                name = "Workout A",
                slots = listOf(
                    WorkoutExerciseSlot(
                        id = 11L,
                        workoutId = 5L,
                        exerciseId = 1006L,
                        position = 0,
                        targetSets = 1,
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
        )
        val sessionSummaryRepository = FakeSessionSummaryRepository(
            summary = WorkoutSessionSummary(
                sessionId = 42L,
                workoutId = 5L,
                totalDurationSeconds = 600L,
                totalVolumeKg = 500.0,
                volumeRule = "sum(reps * weight)",
                sets = emptyList(),
            ),
        )
        val workoutSummaryRepository = FakeWorkoutSummaryRepository()
        val completeSessionAndSaveSummary = CompleteSessionAndSaveSummaryUseCase(
            completeSessionWithProgressionUseCase = CompleteSessionWithProgressionUseCase(
                sessionRepository = sessionRepository,
                workoutRepository = workoutRepository,
                progressionCalculator = ProgressionCalculator(),
            ),
            sessionSummaryRepository = sessionSummaryRepository,
            workoutSummaryRepository = workoutSummaryRepository,
            workoutRepository = workoutRepository,
        )
        val viewModel = createViewModel(
            sessionRepository = sessionRepository,
            completeSessionAndSaveSummary = completeSessionAndSaveSummary,
        )

        runCurrent()
        val result = viewModel.finishSession()
        runCurrent()

        assertEquals(42L, result)
        assertEquals(1, sessionRepository.completeWithProgressionCalls)
        assertTrue(viewModel.uiState.value.isCompleted)
        assertEquals(listOf(42L), workoutSummaryRepository.savedSummaries.map { it.sessionId })
    }

    @Test
    fun onExitRequestedDiscardsOnlyZeroProgressSessions() = runTest {
        val withoutProgress = ActiveWorkoutFakeSessionRepository(
            activeState = activeState(
                plannedSets = listOf(
                    plannedSet(id = 1, workoutSlotId = 11, setOrder = 0, exerciseId = 1006, setType = SessionSetType.WORK, targetReps = 5),
                ),
            ),
        )
        val withoutProgressViewModel = createViewModel(sessionRepository = withoutProgress)
        runCurrent()
        withoutProgressViewModel.onExitRequested()
        runCurrent()
        assertEquals(1, withoutProgress.discardCalls)

        val withProgress = ActiveWorkoutFakeSessionRepository(
            activeState = activeState(
                plannedSets = listOf(
                    plannedSet(
                        id = 2,
                        workoutSlotId = 11,
                        setOrder = 0,
                        exerciseId = 1006,
                        setType = SessionSetType.WORK,
                        targetReps = 5,
                        completedReps = 5,
                        isCompleted = true,
                    ),
                ),
            ),
        )
        val withProgressViewModel = createViewModel(sessionRepository = withProgress)
        runCurrent()
        withProgressViewModel.onExitRequested()
        runCurrent()
        assertEquals(0, withProgress.discardCalls)
    }

    private fun createViewModel(
        sessionRepository: ActiveWorkoutFakeSessionRepository,
        timeProvider: TimeProvider = MutableTimeProvider(nowMs = 1_000L),
        completeSessionAndSaveSummary: CompleteSessionAndSaveSummaryUseCase = completeSessionUseCase(sessionRepository),
    ): ActiveWorkoutViewModel =
        ActiveWorkoutViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(AppDestination.ActiveWorkout.SESSION_ID_ARG to sessionRepository.activeState.session.id.toString()),
            ),
            sessionRepository = sessionRepository,
            exerciseRepository = FakeExerciseRepository(
                exercises = listOf(Exercise(id = 1006, name = "Deadlift", primaryMuscleGroup = "BACK", secondaryMuscleGroups = emptyList(), equipmentType = "BARBELL")),
            ),
            settingsRepository = FakeSettingsRepository(),
            completeSessionAndSaveSummary = completeSessionAndSaveSummary,
            restTimerPolicy = RestTimerPolicy(),
            restTimerCalculator = RestTimerCalculator(),
            elapsedTimeCalculator = ElapsedTimeCalculator(),
            restSignalPlayer = RecordingRestSignalPlayer(),
            timeProvider = timeProvider,
        )

    private fun completeSessionUseCase(sessionRepository: SessionRepository): CompleteSessionAndSaveSummaryUseCase {
        val workoutRepository = FakeWorkoutRepository()
        return CompleteSessionAndSaveSummaryUseCase(
            completeSessionWithProgressionUseCase = CompleteSessionWithProgressionUseCase(
                sessionRepository = sessionRepository,
                workoutRepository = workoutRepository,
                progressionCalculator = ProgressionCalculator(),
            ),
            sessionSummaryRepository = FakeSessionSummaryRepository(),
            workoutSummaryRepository = FakeWorkoutSummaryRepository(),
            workoutRepository = workoutRepository,
        )
    }
}

private class ActiveWorkoutFakeSessionRepository(
    var activeState: ActiveSessionState,
    private val setResults: List<SetResult> = emptyList(),
) : SessionRepository {
    private var nextSetId = (activeState.plannedSets.maxOfOrNull { it.id } ?: 0L) + 1L
    var completeWithProgressionCalls: Int = 0
    var discardCalls: Int = 0

    override suspend fun startSession(workoutId: Long, plannedSets: List<SessionPlannedSet>): Long = activeState.session.id
    override suspend fun findUnfinishedSessionId(): Long? = activeState.session.id

    override suspend fun getActiveSessionState(sessionId: Long): ActiveSessionState = activeState

    override suspend fun discardSessionIfNoProgress(sessionId: Long): Boolean {
        val hasProgress = activeState.plannedSets.any { it.isCompleted || (it.completedReps ?: 0) > 0 }
        if (hasProgress) return false
        discardCalls += 1
        return true
    }

    override suspend fun completePlannedSet(
        sessionId: Long,
        plannedSetId: Long,
        repsAchieved: Int,
    ): ActiveSessionState {
        activeState = activeState.copy(
            plannedSets = activeState.plannedSets.map { plannedSet ->
                if (plannedSet.id != plannedSetId) {
                    plannedSet
                } else {
                    val normalized = repsAchieved.coerceAtLeast(0)
                    plannedSet.copy(
                        isCompleted = normalized > 0,
                        completedReps = normalized.takeIf { it > 0 },
                    )
                }
            },
        )
        return activeState
    }

    override suspend fun updatePlannedSetWeight(
        sessionId: Long,
        plannedSetId: Long,
        weightKg: Double,
    ): ActiveSessionState {
        activeState = activeState.copy(
            plannedSets = activeState.plannedSets.map { plannedSet ->
                if (plannedSet.id == plannedSetId) plannedSet.copy(targetWeightKg = weightKg) else plannedSet
            },
        )
        return activeState
    }

    override suspend fun addExtraSet(sessionId: Long, anchorPlannedSetId: Long): ActiveSessionState {
        val reordered = buildList {
            activeState.plannedSets.forEach { plannedSet ->
                add(plannedSet)
                if (plannedSet.id == anchorPlannedSetId) {
                    add(
                        plannedSet.copy(
                            id = nextSetId++,
                            isCompleted = false,
                            completedReps = null,
                            isExtra = true,
                        ),
                    )
                }
            }
        }.mapIndexed { index, plannedSet -> plannedSet.copy(setOrder = index) }
        activeState = activeState.copy(plannedSets = reordered)
        return activeState
    }

    override suspend fun removeExtraSet(sessionId: Long, plannedSetId: Long): ActiveSessionState {
        activeState = activeState.copy(
            plannedSets = activeState.plannedSets
                .filterNot { it.id == plannedSetId && it.isExtra }
                .mapIndexed { index, plannedSet -> plannedSet.copy(setOrder = index) },
        )
        return activeState
    }

    override suspend fun completeSession(sessionId: Long) = Unit

    override suspend fun completeSessionWithProgression(sessionId: Long, updates: List<SlotProgressionUpdate>) {
        completeWithProgressionCalls += 1
        activeState = activeState.copy(
            session = activeState.session.copy(endedAtEpochMs = 2_000L),
        )
    }

    override suspend fun saveSession(session: WorkoutSession): Long = session.id

    override suspend fun saveSetResult(result: SetResult): Long = result.id

    override suspend fun getSetResults(sessionId: Long): List<SetResult> = setResults
}

private class FakeExerciseRepository(
    private val exercises: List<Exercise>,
) : ExerciseRepository {
    override suspend fun save(exercise: Exercise): Long = exercise.id

    override suspend fun getById(exerciseId: Long): Exercise? = exercises.firstOrNull { it.id == exerciseId }

    override suspend fun getAll(): List<Exercise> = exercises

    override suspend fun delete(exerciseId: Long) = Unit
}

private class FakeSettingsRepository : SettingsRepository {
    private val state = MutableStateFlow(
        AppSettings(
            restDurationSeconds = 180,
            loadIncrementKg = 2.5,
            deloadPercent = 10,
            defaultProgressionMode = "WEIGHT_ONLY",
        ),
    )

    override val settings: Flow<AppSettings> = state

    override suspend fun updateDefaults(
        restDurationSeconds: Int,
        loadIncrementKg: Double,
        deloadPercent: Int,
        defaultProgressionMode: String,
    ) = Unit
}

private class FakeWorkoutRepository(
    private val workout: Workout? = null,
) : WorkoutRepository {
    override suspend fun createWorkout(workout: Workout): Long = workout.id

    override suspend fun updateWorkout(workout: Workout) = Unit

    override suspend fun deleteWorkout(workoutId: Long) = Unit

    override suspend fun getWorkout(workoutId: Long): Workout? = workout?.takeIf { it.id == workoutId }

    override suspend fun getAllWorkouts(): List<Workout> = listOfNotNull(workout)
}

private class FakeSessionSummaryRepository(
    private val summary: WorkoutSessionSummary? = null,
) : SessionSummaryRepository {
    override suspend fun getSessionSummary(sessionId: Long): WorkoutSessionSummary? = summary
    override suspend fun getExerciseHistory(exerciseId: Long): List<ExerciseHistoryEntry> = emptyList()
    override suspend fun getAllExerciseHistory(): List<ExerciseHistoryEntry> = emptyList()
}

private class FakeWorkoutSummaryRepository : WorkoutSummaryRepository {
    val savedSummaries = mutableListOf<WorkoutSummary>()

    override suspend fun saveSummary(summary: WorkoutSummary): Long {
        savedSummaries += summary
        return summary.sessionId
    }

    override suspend fun getAllSummaries(): List<WorkoutSummary> = emptyList()

    override suspend fun getSummaryBySessionId(sessionId: Long): WorkoutSummary? = null

    override suspend fun getHistory(): List<WorkoutHistoryItem> = emptyList()
}

private class RecordingRestSignalPlayer : RestSignalPlayer {
    var playCalls: Int = 0

    override fun playRestOverSignal() {
        playCalls += 1
    }
}

private data class MutableTimeProvider(
    var nowMs: Long,
) : TimeProvider {
    override fun nowMs(): Long = nowMs
}

private fun activeState(
    plannedSets: List<SessionPlannedSet>,
    endedAtEpochMs: Long? = 1_000L,
): ActiveSessionState =
    ActiveSessionState(
        session = WorkoutSession(
            id = 42L,
            workoutId = 5L,
            startedAtEpochMs = 1_000L,
            endedAtEpochMs = endedAtEpochMs,
        ),
        plannedSets = plannedSets,
    )

private fun plannedSet(
    id: Long,
    workoutSlotId: Long,
    setOrder: Int,
    exerciseId: Long,
    setType: String,
    targetReps: Int,
    targetWeightKg: Double? = null,
    completedReps: Int? = null,
    isCompleted: Boolean = false,
    isExtra: Boolean = false,
): SessionPlannedSet =
    SessionPlannedSet(
        id = id,
        sessionId = 42L,
        workoutSlotId = workoutSlotId,
        setOrder = setOrder,
        exerciseId = exerciseId,
        setType = setType,
        targetReps = targetReps,
        targetWeightKg = targetWeightKg,
        isCompleted = isCompleted,
        completedReps = completedReps,
        isExtra = isExtra,
    )
