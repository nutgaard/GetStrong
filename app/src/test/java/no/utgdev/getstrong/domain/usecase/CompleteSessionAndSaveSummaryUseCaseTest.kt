package no.utgdev.getstrong.domain.usecase

import kotlinx.coroutines.test.runTest
import no.utgdev.getstrong.domain.model.ActiveSessionState
import no.utgdev.getstrong.domain.model.ExerciseHistoryEntry
import no.utgdev.getstrong.domain.model.ProgressionModeCode
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.SessionSetType
import no.utgdev.getstrong.domain.model.SetResult
import no.utgdev.getstrong.domain.model.SlotProgressionUpdate
import no.utgdev.getstrong.domain.model.Workout
import no.utgdev.getstrong.domain.model.WorkoutExerciseSlot
import no.utgdev.getstrong.domain.model.WorkoutHistoryItem
import no.utgdev.getstrong.domain.model.WorkoutSession
import no.utgdev.getstrong.domain.model.WorkoutSessionSummary
import no.utgdev.getstrong.domain.model.WorkoutSummary
import no.utgdev.getstrong.domain.repository.SessionRepository
import no.utgdev.getstrong.domain.repository.SessionSummaryRepository
import no.utgdev.getstrong.domain.repository.WorkoutRepository
import no.utgdev.getstrong.domain.repository.WorkoutSummaryRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class CompleteSessionAndSaveSummaryUseCaseTest {
    @Test
    fun usesRepositoryAtomicPathAndDoesNotDuplicateHistory() = runTest {
        val sessionId = 33L
        val workoutId = 7L
        val workoutSummaryRepository = SummaryCapturingWorkoutSummaryRepository()
        val sessionRepository = SummaryCapturingSessionRepository(
            activeState = activeState(sessionId, workoutId),
            setResults = completedSetResults(sessionId),
            persistSummaryFromAtomicPath = true,
            workoutSummaryRepository = workoutSummaryRepository,
        )
        val workoutRepository = FixedWorkoutRepository(workoutId = workoutId)
        val useCase = CompleteSessionAndSaveSummaryUseCase(
            completeSessionWithProgressionUseCase = CompleteSessionWithProgressionUseCase(
                sessionRepository = sessionRepository,
                workoutRepository = workoutRepository,
                progressionCalculator = ProgressionCalculator(),
            ),
            sessionRepository = sessionRepository,
            sessionSummaryRepository = FixedSessionSummaryRepository(sessionId, workoutId),
            workoutSummaryRepository = workoutSummaryRepository,
            workoutRepository = workoutRepository,
        )

        useCase(sessionId)
        useCase(sessionId)

        assertEquals(2, sessionRepository.completeWithSummaryCalls)
        assertEquals(1, workoutSummaryRepository.storedBySessionId.size)
        assertEquals(0, workoutSummaryRepository.saveSummaryCalls)
    }

    @Test
    fun fallsBackToLegacySummarySaveWhenAtomicPathDoesNotPersistProjection() = runTest {
        val sessionId = 33L
        val workoutId = 7L
        val workoutSummaryRepository = SummaryCapturingWorkoutSummaryRepository()
        val sessionRepository = SummaryCapturingSessionRepository(
            activeState = activeState(sessionId, workoutId),
            setResults = completedSetResults(sessionId),
            persistSummaryFromAtomicPath = false,
            workoutSummaryRepository = workoutSummaryRepository,
        )
        val workoutRepository = FixedWorkoutRepository(workoutId = workoutId)
        val useCase = CompleteSessionAndSaveSummaryUseCase(
            completeSessionWithProgressionUseCase = CompleteSessionWithProgressionUseCase(
                sessionRepository = sessionRepository,
                workoutRepository = workoutRepository,
                progressionCalculator = ProgressionCalculator(),
            ),
            sessionRepository = sessionRepository,
            sessionSummaryRepository = FixedSessionSummaryRepository(sessionId, workoutId),
            workoutSummaryRepository = workoutSummaryRepository,
            workoutRepository = workoutRepository,
        )

        useCase(sessionId)

        assertEquals(1, sessionRepository.completeWithSummaryCalls)
        assertEquals(1, workoutSummaryRepository.storedBySessionId.size)
        assertEquals(1, workoutSummaryRepository.saveSummaryCalls)
    }
}

private class SummaryCapturingSessionRepository(
    private val activeState: ActiveSessionState,
    private val setResults: List<SetResult>,
    private val persistSummaryFromAtomicPath: Boolean,
    private val workoutSummaryRepository: SummaryCapturingWorkoutSummaryRepository,
) : SessionRepository {
    var completeWithSummaryCalls: Int = 0

    override suspend fun startSession(workoutId: Long, plannedSets: List<SessionPlannedSet>): Long = activeState.session.id
    override suspend fun findUnfinishedSessionId(): Long? = null
    override suspend fun discardSessionIfNoProgress(sessionId: Long): Boolean = false
    override suspend fun getActiveSessionState(sessionId: Long): ActiveSessionState = activeState
    override suspend fun completePlannedSet(sessionId: Long, plannedSetId: Long, repsAchieved: Int): ActiveSessionState = activeState
    override suspend fun updatePlannedSetWeight(sessionId: Long, plannedSetId: Long, weightKg: Double): ActiveSessionState = activeState
    override suspend fun addExtraSet(sessionId: Long, anchorPlannedSetId: Long): ActiveSessionState = activeState
    override suspend fun removeExtraSet(sessionId: Long, plannedSetId: Long): ActiveSessionState = activeState
    override suspend fun completeSession(sessionId: Long) = Unit
    override suspend fun completeSessionWithProgression(sessionId: Long, updates: List<SlotProgressionUpdate>) = Unit

    override suspend fun completeSessionWithProgressionAndPersistSummary(
        sessionId: Long,
        updates: List<SlotProgressionUpdate>,
    ): Boolean {
        completeWithSummaryCalls += 1
        if (persistSummaryFromAtomicPath) {
            workoutSummaryRepository.upsertFromAtomicPath(
                WorkoutSummary(
                    workoutId = activeState.session.workoutId,
                    sessionId = sessionId,
                    workoutName = "Workout ${activeState.session.workoutId}",
                    totalVolumeKg = 500.0,
                    totalDurationSeconds = 600L,
                    completedAtEpochMs = 1_000L,
                ),
            )
        }
        return persistSummaryFromAtomicPath
    }

    override suspend fun saveSession(session: WorkoutSession): Long = session.id
    override suspend fun saveSetResult(result: SetResult): Long = result.id
    override suspend fun getSetResults(sessionId: Long): List<SetResult> = setResults
}

private class SummaryCapturingWorkoutSummaryRepository : WorkoutSummaryRepository {
    var saveSummaryCalls: Int = 0
    val storedBySessionId = linkedMapOf<Long, WorkoutSummary>()

    override suspend fun saveSummary(summary: WorkoutSummary): Long {
        saveSummaryCalls += 1
        storedBySessionId[summary.sessionId] = summary
        return summary.sessionId
    }

    override suspend fun getAllSummaries(): List<WorkoutSummary> = storedBySessionId.values.toList()

    override suspend fun getSummaryBySessionId(sessionId: Long): WorkoutSummary? = storedBySessionId[sessionId]

    override suspend fun getHistory(): List<WorkoutHistoryItem> = storedBySessionId.values.map {
        WorkoutHistoryItem(
            id = it.id,
            sessionId = it.sessionId,
            workoutName = it.workoutName,
            totalVolumeKg = it.totalVolumeKg,
            totalDurationSeconds = it.totalDurationSeconds,
            completedAtEpochMs = it.completedAtEpochMs,
        )
    }

    fun upsertFromAtomicPath(summary: WorkoutSummary) {
        storedBySessionId[summary.sessionId] = summary
    }
}

private class FixedWorkoutRepository(
    private val workoutId: Long,
) : WorkoutRepository {
    override suspend fun createWorkout(workout: Workout): Long = workout.id
    override suspend fun updateWorkout(workout: Workout) = Unit
    override suspend fun deleteWorkout(workoutId: Long) = Unit
    override suspend fun getWorkout(workoutId: Long): Workout? =
        if (workoutId != this.workoutId) {
            null
        } else {
            Workout(
                id = workoutId,
                name = "Workout $workoutId",
                slots = listOf(
                    WorkoutExerciseSlot(
                        id = 88L,
                        workoutId = workoutId,
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
            )
        }

    override suspend fun getAllWorkouts(): List<Workout> = listOfNotNull(getWorkout(workoutId))
}

private class FixedSessionSummaryRepository(
    private val sessionId: Long,
    private val workoutId: Long,
) : SessionSummaryRepository {
    override suspend fun getSessionSummary(sessionId: Long): WorkoutSessionSummary? =
        if (sessionId != this.sessionId) {
            null
        } else {
            WorkoutSessionSummary(
                sessionId = sessionId,
                workoutId = workoutId,
                totalVolumeKg = 500.0,
                totalDurationSeconds = 600L,
                volumeRule = "WORK_SETS_ONLY",
                sets = emptyList(),
            )
        }

    override suspend fun getExerciseHistory(exerciseId: Long): List<ExerciseHistoryEntry> = emptyList()
    override suspend fun getAllExerciseHistory(): List<ExerciseHistoryEntry> = emptyList()
}

private fun activeState(sessionId: Long, workoutId: Long): ActiveSessionState =
    ActiveSessionState(
        session = WorkoutSession(
            id = sessionId,
            workoutId = workoutId,
            startedAtEpochMs = 1L,
            endedAtEpochMs = null,
        ),
        plannedSets = listOf(
            SessionPlannedSet(
                id = 1L,
                sessionId = sessionId,
                workoutSlotId = 88L,
                setOrder = 0,
                exerciseId = 1006L,
                setType = SessionSetType.WORK,
                targetReps = 5,
                targetWeightKg = 100.0,
                isCompleted = true,
                completedReps = 5,
            ),
        ),
    )

private fun completedSetResults(sessionId: Long): List<SetResult> =
    listOf(
        SetResult(
            id = 1L,
            sessionId = sessionId,
            plannedSetId = 1L,
            workoutSlotId = 88L,
            exerciseId = 1006L,
            setType = SessionSetType.WORK,
            reps = 5,
            weightKg = 100.0,
        ),
    )
