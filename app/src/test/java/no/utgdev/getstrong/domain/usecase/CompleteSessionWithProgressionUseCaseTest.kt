package no.utgdev.getstrong.domain.usecase

import kotlinx.coroutines.test.runTest
import no.utgdev.getstrong.domain.model.ActiveSessionState
import no.utgdev.getstrong.domain.model.ProgressionModeCode
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.SessionSetType
import no.utgdev.getstrong.domain.model.SetResult
import no.utgdev.getstrong.domain.model.SlotProgressionUpdate
import no.utgdev.getstrong.domain.model.Workout
import no.utgdev.getstrong.domain.model.WorkoutExerciseSlot
import no.utgdev.getstrong.domain.model.WorkoutSession
import no.utgdev.getstrong.domain.repository.SessionRepository
import no.utgdev.getstrong.domain.repository.WorkoutRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class CompleteSessionWithProgressionUseCaseTest {
    @Test
    fun appliesProgressionOnceForSameSession() = runTest {
        val workoutId = 10L
        val slotId = 20L
        val exerciseId = 1006L
        val sessionId = 30L

        val workoutRepository = FakeWorkoutRepository(
            Workout(
                id = workoutId,
                name = "A",
                slots = listOf(
                    WorkoutExerciseSlot(
                        id = slotId,
                        workoutId = workoutId,
                        exerciseId = exerciseId,
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
        )
        val sessionRepository = FakeSessionRepository(
            activeState = ActiveSessionState(
                session = WorkoutSession(id = sessionId, workoutId = workoutId, startedAtEpochMs = 1L),
                plannedSets = listOf(
                    SessionPlannedSet(
                        id = 1,
                        sessionId = sessionId,
                        workoutSlotId = slotId,
                        setOrder = 0,
                        exerciseId = exerciseId,
                        setType = SessionSetType.WORK,
                        targetReps = 5,
                        isCompleted = true,
                        completedReps = 5,
                    ),
                ),
            ),
            results = listOf(
                SetResult(
                    id = 1,
                    sessionId = sessionId,
                    workoutSlotId = slotId,
                    exerciseId = exerciseId,
                    setType = SessionSetType.WORK,
                    reps = 5,
                    weightKg = 100.0,
                ),
                SetResult(
                    id = 2,
                    sessionId = sessionId,
                    workoutSlotId = slotId,
                    exerciseId = exerciseId,
                    setType = SessionSetType.WORK,
                    reps = 5,
                    weightKg = 100.0,
                ),
            ),
        )
        sessionRepository.onCompleteWithProgression = { _, updates ->
            workoutRepository.applySlotUpdates(sessionId, updates)
        }

        val useCase = CompleteSessionWithProgressionUseCase(
            sessionRepository = sessionRepository,
            workoutRepository = workoutRepository,
            progressionCalculator = ProgressionCalculator(),
        )

        useCase(sessionId)
        useCase(sessionId)

        assertEquals(2, sessionRepository.completeCalls)
        assertEquals(1, sessionRepository.completedUpdates.sumOf { it.size })
        val update = sessionRepository.completedUpdates.first().first()
        assertEquals(slotId, update.slotId)
        assertEquals(102.5, update.nextWorkingWeightKg, 0.0)
        assertEquals(0, update.nextFailureStreak)
    }

    @Test
    fun incrementsFailureStreakAndDeloadsOnThirdFailure() = runTest {
        val workoutId = 10L
        val slotId = 20L
        val exerciseId = 1006L
        val sessionId = 30L

        val workoutRepository = FakeWorkoutRepository(
            Workout(
                id = workoutId,
                name = "A",
                slots = listOf(
                    WorkoutExerciseSlot(
                        id = slotId,
                        workoutId = workoutId,
                        exerciseId = exerciseId,
                        position = 0,
                        targetSets = 2,
                        targetReps = 5,
                        repRangeMin = 5,
                        repRangeMax = 5,
                        progressionMode = ProgressionModeCode.WEIGHT_ONLY,
                        incrementKg = 2.5,
                        deloadPercent = 10,
                        currentWorkingWeightKg = 100.0,
                        failureStreak = 2,
                        restSecondsOverride = null,
                    ),
                ),
            ),
        )
        val sessionRepository = FakeSessionRepository(
            activeState = ActiveSessionState(
                session = WorkoutSession(id = sessionId, workoutId = workoutId, startedAtEpochMs = 1L),
                plannedSets = listOf(
                    SessionPlannedSet(
                        id = 1,
                        sessionId = sessionId,
                        workoutSlotId = slotId,
                        setOrder = 0,
                        exerciseId = exerciseId,
                        setType = SessionSetType.WORK,
                        targetReps = 5,
                        isCompleted = true,
                        completedReps = 4,
                    ),
                ),
            ),
            results = listOf(
                SetResult(
                    id = 1,
                    sessionId = sessionId,
                    workoutSlotId = slotId,
                    exerciseId = exerciseId,
                    setType = SessionSetType.WORK,
                    reps = 4,
                    weightKg = 100.0,
                ),
                SetResult(
                    id = 2,
                    sessionId = sessionId,
                    workoutSlotId = slotId,
                    exerciseId = exerciseId,
                    setType = SessionSetType.WORK,
                    reps = 4,
                    weightKg = 100.0,
                ),
            ),
        )

        val useCase = CompleteSessionWithProgressionUseCase(
            sessionRepository = sessionRepository,
            workoutRepository = workoutRepository,
            progressionCalculator = ProgressionCalculator(),
        )

        useCase(sessionId)

        val update = sessionRepository.completedUpdates.single().single()
        assertEquals(slotId, update.slotId)
        assertEquals(90.0, update.nextWorkingWeightKg, 0.0)
        assertEquals(0, update.nextFailureStreak)
    }

    @Test
    fun incrementsFailureStreakWithoutDeloadBeforeThirdFailure() = runTest {
        val workoutId = 10L
        val slotId = 20L
        val exerciseId = 1006L
        val sessionId = 30L

        val workoutRepository = FakeWorkoutRepository(
            Workout(
                id = workoutId,
                name = "A",
                slots = listOf(
                    WorkoutExerciseSlot(
                        id = slotId,
                        workoutId = workoutId,
                        exerciseId = exerciseId,
                        position = 0,
                        targetSets = 2,
                        targetReps = 5,
                        repRangeMin = 5,
                        repRangeMax = 5,
                        progressionMode = ProgressionModeCode.WEIGHT_ONLY,
                        incrementKg = 2.5,
                        deloadPercent = 10,
                        currentWorkingWeightKg = 100.0,
                        failureStreak = 1,
                        restSecondsOverride = null,
                    ),
                ),
            ),
        )
        val sessionRepository = FakeSessionRepository(
            activeState = ActiveSessionState(
                session = WorkoutSession(id = sessionId, workoutId = workoutId, startedAtEpochMs = 1L),
                plannedSets = listOf(
                    SessionPlannedSet(
                        id = 1,
                        sessionId = sessionId,
                        workoutSlotId = slotId,
                        setOrder = 0,
                        exerciseId = exerciseId,
                        setType = SessionSetType.WORK,
                        targetReps = 5,
                        isCompleted = true,
                        completedReps = 4,
                    ),
                ),
            ),
            results = listOf(
                SetResult(
                    id = 1,
                    sessionId = sessionId,
                    workoutSlotId = slotId,
                    exerciseId = exerciseId,
                    setType = SessionSetType.WORK,
                    reps = 4,
                    weightKg = 100.0,
                ),
                SetResult(
                    id = 2,
                    sessionId = sessionId,
                    workoutSlotId = slotId,
                    exerciseId = exerciseId,
                    setType = SessionSetType.WORK,
                    reps = 4,
                    weightKg = 100.0,
                ),
            ),
        )

        val useCase = CompleteSessionWithProgressionUseCase(
            sessionRepository = sessionRepository,
            workoutRepository = workoutRepository,
            progressionCalculator = ProgressionCalculator(),
        )

        useCase(sessionId)

        val update = sessionRepository.completedUpdates.single().single()
        assertEquals(slotId, update.slotId)
        assertEquals(100.0, update.nextWorkingWeightKg, 0.0)
        assertEquals(2, update.nextFailureStreak)
    }

    @Test
    fun ignoresWarmupSetResultsWhenEvaluatingFailureAndProgression() = runTest {
        val workoutId = 10L
        val slotId = 20L
        val exerciseId = 1006L
        val sessionId = 30L

        val workoutRepository = FakeWorkoutRepository(
            Workout(
                id = workoutId,
                name = "A",
                slots = listOf(
                    WorkoutExerciseSlot(
                        id = slotId,
                        workoutId = workoutId,
                        exerciseId = exerciseId,
                        position = 0,
                        targetSets = 2,
                        targetReps = 5,
                        repRangeMin = 5,
                        repRangeMax = 5,
                        progressionMode = ProgressionModeCode.WEIGHT_ONLY,
                        incrementKg = 2.5,
                        deloadPercent = 10,
                        currentWorkingWeightKg = 100.0,
                        failureStreak = 1,
                        restSecondsOverride = null,
                    ),
                ),
            ),
        )
        val sessionRepository = FakeSessionRepository(
            activeState = ActiveSessionState(
                session = WorkoutSession(id = sessionId, workoutId = workoutId, startedAtEpochMs = 1L),
                plannedSets = listOf(
                    SessionPlannedSet(
                        id = 1,
                        sessionId = sessionId,
                        workoutSlotId = slotId,
                        setOrder = 0,
                        exerciseId = exerciseId,
                        setType = SessionSetType.WARMUP,
                        targetReps = 3,
                        isCompleted = true,
                        completedReps = 2,
                    ),
                ),
            ),
            results = listOf(
                SetResult(
                    id = 1,
                    sessionId = sessionId,
                    workoutSlotId = slotId,
                    exerciseId = exerciseId,
                    setType = SessionSetType.WARMUP,
                    reps = 1,
                    weightKg = 60.0,
                ),
                SetResult(
                    id = 2,
                    sessionId = sessionId,
                    workoutSlotId = slotId,
                    exerciseId = exerciseId,
                    setType = SessionSetType.WORK,
                    reps = 5,
                    weightKg = 100.0,
                ),
                SetResult(
                    id = 3,
                    sessionId = sessionId,
                    workoutSlotId = slotId,
                    exerciseId = exerciseId,
                    setType = SessionSetType.WORK,
                    reps = 5,
                    weightKg = 100.0,
                ),
            ),
        )

        val useCase = CompleteSessionWithProgressionUseCase(
            sessionRepository = sessionRepository,
            workoutRepository = workoutRepository,
            progressionCalculator = ProgressionCalculator(),
        )

        useCase(sessionId)

        val update = sessionRepository.completedUpdates.single().single()
        assertEquals(slotId, update.slotId)
        assertEquals(102.5, update.nextWorkingWeightKg, 0.0)
        assertEquals(0, update.nextFailureStreak)
    }
}

private class FakeSessionRepository(
    private val activeState: ActiveSessionState,
    private val results: List<SetResult>,
) : SessionRepository {
    var completeCalls: Int = 0
    val completedUpdates: MutableList<List<SlotProgressionUpdate>> = mutableListOf()
    var onCompleteWithProgression: ((Long, List<SlotProgressionUpdate>) -> Unit)? = null

    override suspend fun startSession(workoutId: Long, plannedSets: List<SessionPlannedSet>): Long = 0L

    override suspend fun getActiveSessionState(sessionId: Long): ActiveSessionState = activeState

    override suspend fun completePlannedSet(
        sessionId: Long,
        plannedSetId: Long,
        repsAchieved: Int,
    ): ActiveSessionState = activeState

    override suspend fun updatePlannedSetWeight(
        sessionId: Long,
        plannedSetId: Long,
        weightKg: Double,
    ): ActiveSessionState = activeState

    override suspend fun addExtraSet(sessionId: Long, anchorPlannedSetId: Long): ActiveSessionState = activeState

    override suspend fun removeExtraSet(sessionId: Long, plannedSetId: Long): ActiveSessionState = activeState

    override suspend fun completeSession(sessionId: Long) = Unit

    override suspend fun completeSessionWithProgression(sessionId: Long, updates: List<SlotProgressionUpdate>) {
        completeCalls += 1
        completedUpdates += updates
        onCompleteWithProgression?.invoke(sessionId, updates)
    }

    override suspend fun saveSession(session: WorkoutSession): Long = session.id

    override suspend fun saveSetResult(result: SetResult): Long = result.id

    override suspend fun getSetResults(sessionId: Long): List<SetResult> = results
}

private class FakeWorkoutRepository(
    private var workout: Workout,
) : WorkoutRepository {
    override suspend fun createWorkout(workout: Workout): Long = workout.id

    override suspend fun updateWorkout(workout: Workout) {
        this.workout = workout
    }

    override suspend fun deleteWorkout(workoutId: Long) = Unit

    override suspend fun getWorkout(workoutId: Long): Workout? = workout.takeIf { it.id == workoutId }

    override suspend fun getAllWorkouts(): List<Workout> = listOf(workout)

    fun applySlotUpdates(sessionId: Long, updates: List<SlotProgressionUpdate>) {
        if (updates.isEmpty()) return
        val mapped = workout.slots.map { slot ->
            val update = updates.firstOrNull { it.slotId == slot.id } ?: return@map slot
            slot.copy(
                targetReps = update.nextTargetReps,
                currentWorkingWeightKg = update.nextWorkingWeightKg,
                failureStreak = update.nextFailureStreak,
                lastProgressionSessionId = sessionId,
            )
        }
        workout = workout.copy(slots = mapped)
    }
}
