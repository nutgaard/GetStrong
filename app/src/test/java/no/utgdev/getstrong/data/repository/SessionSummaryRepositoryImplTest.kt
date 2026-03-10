package no.utgdev.getstrong.data.repository

import kotlinx.coroutines.test.runTest
import no.utgdev.getstrong.data.local.dao.SessionDao
import no.utgdev.getstrong.data.local.dao.SlotProgressionRecord
import no.utgdev.getstrong.data.local.entity.SessionPlannedSetEntity
import no.utgdev.getstrong.data.local.entity.SetResultEntity
import no.utgdev.getstrong.data.local.entity.WorkoutSessionEntity
import no.utgdev.getstrong.domain.model.SessionSetType
import no.utgdev.getstrong.domain.usecase.ElapsedTimeCalculator
import no.utgdev.getstrong.domain.usecase.SessionSummaryCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SessionSummaryRepositoryImplTest {
    @Test
    fun projectsPersistedRowsAndTotalsForCompletedSession() = runTest {
        val dao = FakeSessionDaoForSummary()
        val sessionId = 7L
        dao.sessions[sessionId] = WorkoutSessionEntity(
            id = sessionId,
            workoutId = 2,
            startedAtEpochMs = 1000L,
            endedAtEpochMs = 31_000L,
        )
        dao.planned[sessionId] = mutableListOf(
            SessionPlannedSetEntity(
                id = 1,
                sessionId = sessionId,
                workoutSlotId = 11,
                setOrder = 0,
                exerciseId = 1006,
                setType = SessionSetType.WARMUP,
                targetReps = 3,
                targetWeightKg = 60.0,
                isCompleted = true,
                completedReps = 3,
                isExtra = false,
            ),
            SessionPlannedSetEntity(
                id = 2,
                sessionId = sessionId,
                workoutSlotId = 11,
                setOrder = 1,
                exerciseId = 1006,
                setType = SessionSetType.WORK,
                targetReps = 5,
                targetWeightKg = 100.0,
                isCompleted = true,
                completedReps = 5,
                isExtra = false,
            ),
        )
        dao.results[sessionId] = mutableListOf(
            SetResultEntity(
                id = 1,
                sessionId = sessionId,
                plannedSetId = 1,
                workoutSlotId = 11,
                exerciseId = 1006,
                setType = SessionSetType.WARMUP,
                reps = 3,
                weightKg = 60.0,
            ),
            SetResultEntity(
                id = 2,
                sessionId = sessionId,
                plannedSetId = 2,
                workoutSlotId = 11,
                exerciseId = 1006,
                setType = SessionSetType.WORK,
                reps = 5,
                weightKg = 100.0,
            ),
        )

        val repository = SessionSummaryRepositoryImpl(
            sessionDao = dao,
            sessionSummaryCalculator = SessionSummaryCalculator(ElapsedTimeCalculator()),
        )

        val summary = repository.getSessionSummary(sessionId)

        assertNotNull(summary)
        assertEquals(30L, summary!!.totalDurationSeconds)
        assertEquals(500.0, summary.totalVolumeKg, 0.0)
        assertEquals(2, summary.sets.size)
        assertEquals(SessionSetType.WARMUP, summary.sets[0].setType)
        assertEquals(SessionSetType.WORK, summary.sets[1].setType)
    }
}

private class FakeSessionDaoForSummary : SessionDao {
    val sessions = linkedMapOf<Long, WorkoutSessionEntity>()
    val planned = linkedMapOf<Long, MutableList<SessionPlannedSetEntity>>()
    val results = linkedMapOf<Long, MutableList<SetResultEntity>>()

    override suspend fun upsertSession(session: WorkoutSessionEntity): Long = session.id

    override suspend fun upsertSetResult(result: SetResultEntity): Long = result.id

    override suspend fun insertPlannedSets(plannedSets: List<SessionPlannedSetEntity>) = Unit

    override suspend fun getSession(sessionId: Long): WorkoutSessionEntity? = sessions[sessionId]

    override suspend fun getPlannedSets(sessionId: Long): List<SessionPlannedSetEntity> =
        planned[sessionId].orEmpty().sortedBy { it.setOrder }

    override suspend fun getPlannedSet(sessionId: Long, plannedSetId: Long): SessionPlannedSetEntity? =
        planned[sessionId].orEmpty().firstOrNull { it.id == plannedSetId }

    override suspend fun updatePlannedSetCompletion(
        sessionId: Long,
        plannedSetId: Long,
        isCompleted: Boolean,
        completedReps: Int?,
    ) = Unit

    override suspend fun updatePlannedSetWeight(sessionId: Long, plannedSetId: Long, weightKg: Double) = Unit

    override suspend fun markSessionCompleted(sessionId: Long, endedAtEpochMs: Long) = Unit

    override suspend fun applySlotProgressionForSession(
        sessionId: Long,
        slotId: Long,
        nextTargetReps: Int,
        nextWorkingWeightKg: Double,
        nextFailureStreak: Int,
    ) = Unit

    override suspend fun getSetResults(sessionId: Long): List<SetResultEntity> = results[sessionId].orEmpty()

    override suspend fun getSetResultForPlannedSet(sessionId: Long, plannedSetId: Long): SetResultEntity? =
        results[sessionId].orEmpty().firstOrNull { it.plannedSetId == plannedSetId }

    override suspend fun deleteSetResultForPlannedSet(sessionId: Long, plannedSetId: Long) = Unit

    override suspend fun deletePlannedSetsForSession(sessionId: Long) = Unit

    override suspend fun createSessionWithPlan(
        session: WorkoutSessionEntity,
        plannedSets: List<SessionPlannedSetEntity>,
    ): Long = session.id

    override suspend fun completeSessionWithProgression(
        sessionId: Long,
        endedAtEpochMs: Long,
        updates: List<SlotProgressionRecord>,
    ) = Unit
}
