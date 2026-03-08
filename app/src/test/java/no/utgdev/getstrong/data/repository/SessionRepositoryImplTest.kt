package no.utgdev.getstrong.data.repository

import kotlinx.coroutines.test.runTest
import no.utgdev.getstrong.data.local.dao.SessionDao
import no.utgdev.getstrong.data.local.dao.SlotProgressionRecord
import no.utgdev.getstrong.data.local.entity.SessionPlannedSetEntity
import no.utgdev.getstrong.data.local.entity.SetResultEntity
import no.utgdev.getstrong.data.local.entity.WorkoutSessionEntity
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.SessionSetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SessionRepositoryImplTest {
    @Test
    fun startAndReloadSessionPreservesWarmupAndWorkSnapshotOrdering() = runTest {
        val dao = FakeSessionDao()
        val repository = SessionRepositoryImpl(dao)
        val planned = listOf(
            SessionPlannedSet(
                sessionId = 0,
                workoutSlotId = 11,
                setOrder = 0,
                exerciseId = 1006,
                setType = SessionSetType.WARMUP,
                targetReps = 3,
                targetWeightKg = 60.0,
            ),
            SessionPlannedSet(
                sessionId = 0,
                workoutSlotId = 11,
                setOrder = 1,
                exerciseId = 1006,
                setType = SessionSetType.WARMUP,
                targetReps = 3,
                targetWeightKg = 80.0,
            ),
            SessionPlannedSet(
                sessionId = 0,
                workoutSlotId = 11,
                setOrder = 2,
                exerciseId = 1006,
                setType = SessionSetType.WORK,
                targetReps = 5,
                targetWeightKg = 140.0,
            ),
        )

        val sessionId = repository.startSession(1L, planned)
        val loaded = repository.getActiveSessionState(sessionId)

        assertNotNull(loaded)
        assertEquals(listOf(0, 1, 2), loaded!!.plannedSets.map { it.setOrder })
        assertEquals(listOf(SessionSetType.WARMUP, SessionSetType.WARMUP, SessionSetType.WORK), loaded.plannedSets.map { it.setType })
        assertEquals(listOf(60.0, 80.0, 140.0), loaded.plannedSets.map { it.targetWeightKg })
    }
}

private class FakeSessionDao : SessionDao {
    private var nextSessionId = 1L
    private var nextPlannedSetId = 1L
    private var nextSetResultId = 1L
    private val sessions = linkedMapOf<Long, WorkoutSessionEntity>()
    private val plannedSetsBySession = linkedMapOf<Long, MutableList<SessionPlannedSetEntity>>()
    private val setResultsBySession = linkedMapOf<Long, MutableList<SetResultEntity>>()

    override suspend fun upsertSession(session: WorkoutSessionEntity): Long {
        val id = if (session.id == 0L) nextSessionId++ else session.id
        sessions[id] = session.copy(id = id)
        return id
    }

    override suspend fun upsertSetResult(result: SetResultEntity): Long {
        val id = if (result.id == 0L) nextSetResultId++ else result.id
        val normalized = result.copy(id = id)
        val list = setResultsBySession.getOrPut(result.sessionId) { mutableListOf() }
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) {
            list[idx] = normalized
        } else {
            list += normalized
        }
        return id
    }

    override suspend fun insertPlannedSets(plannedSets: List<SessionPlannedSetEntity>) {
        if (plannedSets.isEmpty()) return
        val sessionId = plannedSets.first().sessionId
        val stored = plannedSets.map {
            val id = if (it.id == 0L) nextPlannedSetId++ else it.id
            it.copy(id = id)
        }.toMutableList()
        plannedSetsBySession[sessionId] = stored
    }

    override suspend fun getSession(sessionId: Long): WorkoutSessionEntity? = sessions[sessionId]

    override suspend fun getPlannedSets(sessionId: Long): List<SessionPlannedSetEntity> =
        plannedSetsBySession[sessionId].orEmpty().sortedBy { it.setOrder }

    override suspend fun markPlannedSetCompleted(sessionId: Long, plannedSetId: Long, completedReps: Int) {
        val current = plannedSetsBySession[sessionId] ?: return
        val idx = current.indexOfFirst { it.id == plannedSetId }
        if (idx >= 0) {
            current[idx] = current[idx].copy(isCompleted = true, completedReps = completedReps)
        }
    }

    override suspend fun markSessionCompleted(sessionId: Long, endedAtEpochMs: Long) {
        val current = sessions[sessionId] ?: return
        sessions[sessionId] = current.copy(endedAtEpochMs = endedAtEpochMs)
    }

    override suspend fun applySlotProgressionForSession(
        sessionId: Long,
        slotId: Long,
        nextTargetReps: Int,
        nextWorkingWeightKg: Double,
        nextFailureStreak: Int,
    ) = Unit

    override suspend fun getSetResults(sessionId: Long): List<SetResultEntity> =
        setResultsBySession[sessionId].orEmpty()

    override suspend fun createSessionWithPlan(
        session: WorkoutSessionEntity,
        plannedSets: List<SessionPlannedSetEntity>,
    ): Long {
        val sessionId = upsertSession(session)
        insertPlannedSets(plannedSets.map { it.copy(sessionId = sessionId) })
        return sessionId
    }

    override suspend fun completeSessionWithProgression(
        sessionId: Long,
        endedAtEpochMs: Long,
        updates: List<SlotProgressionRecord>,
    ) {
        markSessionCompleted(sessionId, endedAtEpochMs)
    }
}
