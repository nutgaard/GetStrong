package no.utgdev.getstrong.data.repository

import kotlinx.coroutines.test.runTest
import no.utgdev.getstrong.data.local.dao.SessionDao
import no.utgdev.getstrong.data.local.dao.SlotProgressionRecord
import no.utgdev.getstrong.data.local.dao.ExerciseHistoryRow
import no.utgdev.getstrong.data.local.entity.SessionPlannedSetEntity
import no.utgdev.getstrong.data.local.entity.SetResultEntity
import no.utgdev.getstrong.data.local.entity.WorkoutSessionEntity
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.SessionSetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRepositoryImplTest {
    @Test
    fun findUnfinishedSessionIdReturnsLatestOpenSession() = runTest {
        val dao = FakeSessionDao()
        val repository = SessionRepositoryImpl(dao)
        val firstSessionId = repository.startSession(
            workoutId = 1L,
            plannedSets = listOf(
                SessionPlannedSet(
                    sessionId = 0,
                    workoutSlotId = 11,
                    setOrder = 0,
                    exerciseId = 1006,
                    setType = SessionSetType.WORK,
                    targetReps = 5,
                    targetWeightKg = 100.0,
                ),
            ),
        )
        val latestSessionId = repository.startSession(
            workoutId = 2L,
            plannedSets = listOf(
                SessionPlannedSet(
                    sessionId = 0,
                    workoutSlotId = 22,
                    setOrder = 0,
                    exerciseId = 1007,
                    setType = SessionSetType.WORK,
                    targetReps = 5,
                    targetWeightKg = 120.0,
                ),
            ),
        )

        repository.completeSession(latestSessionId)
        val unresolved = repository.findUnfinishedSessionId()

        assertEquals(firstSessionId, unresolved)
    }

    @Test
    fun discardSessionIfNoProgressRemovesOnlyEmptySession() = runTest {
        val dao = FakeSessionDao()
        val repository = SessionRepositoryImpl(dao)
        val emptySessionId = repository.startSession(
            workoutId = 1L,
            plannedSets = listOf(
                SessionPlannedSet(
                    sessionId = 0,
                    workoutSlotId = 11,
                    setOrder = 0,
                    exerciseId = 1006,
                    setType = SessionSetType.WORK,
                    targetReps = 5,
                    targetWeightKg = 100.0,
                ),
            ),
        )
        val progressedSessionId = repository.startSession(
            workoutId = 2L,
            plannedSets = listOf(
                SessionPlannedSet(
                    sessionId = 0,
                    workoutSlotId = 22,
                    setOrder = 0,
                    exerciseId = 1007,
                    setType = SessionSetType.WORK,
                    targetReps = 5,
                    targetWeightKg = 120.0,
                ),
            ),
        )
        val progressedSetId = repository.getActiveSessionState(progressedSessionId)!!.plannedSets.single().id
        repository.completePlannedSet(progressedSessionId, progressedSetId, 5)

        assertTrue(repository.discardSessionIfNoProgress(emptySessionId))
        assertNull(repository.getActiveSessionState(emptySessionId))
        assertTrue(!repository.discardSessionIfNoProgress(progressedSessionId))
        assertNotNull(repository.getActiveSessionState(progressedSessionId))
    }

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
                isExtra = false,
            ),
            SessionPlannedSet(
                sessionId = 0,
                workoutSlotId = 11,
                setOrder = 1,
                exerciseId = 1006,
                setType = SessionSetType.WARMUP,
                targetReps = 3,
                targetWeightKg = 80.0,
                isExtra = false,
            ),
            SessionPlannedSet(
                sessionId = 0,
                workoutSlotId = 11,
                setOrder = 2,
                exerciseId = 1006,
                setType = SessionSetType.WORK,
                targetReps = 5,
                targetWeightKg = 140.0,
                isExtra = false,
            ),
        )

        val sessionId = repository.startSession(1L, planned)
        val loaded = repository.getActiveSessionState(sessionId)

        assertNotNull(loaded)
        assertEquals(listOf(0, 1, 2), loaded!!.plannedSets.map { it.setOrder })
        assertEquals(listOf(SessionSetType.WARMUP, SessionSetType.WARMUP, SessionSetType.WORK), loaded.plannedSets.map { it.setType })
        assertEquals(listOf(60.0, 80.0, 140.0), loaded.plannedSets.map { it.targetWeightKg })
    }

    @Test
    fun completeSetCanBeClearedBackToZeroAndRemovesStoredResult() = runTest {
        val dao = FakeSessionDao()
        val repository = SessionRepositoryImpl(dao)
        val sessionId = repository.startSession(
            workoutId = 1L,
            plannedSets = listOf(
                SessionPlannedSet(
                    sessionId = 0,
                    workoutSlotId = 11,
                    setOrder = 0,
                    exerciseId = 1006,
                    setType = SessionSetType.WORK,
                    targetReps = 5,
                    targetWeightKg = 100.0,
                ),
            ),
        )
        val plannedSetId = repository.getActiveSessionState(sessionId)!!.plannedSets.single().id

        repository.completePlannedSet(sessionId, plannedSetId, 5)
        repository.completePlannedSet(sessionId, plannedSetId, 0)

        val reloaded = repository.getActiveSessionState(sessionId)!!.plannedSets.single()
        assertTrue(!reloaded.isCompleted)
        assertNull(reloaded.completedReps)
        assertTrue(repository.getSetResults(sessionId).isEmpty())
    }

    @Test
    fun addAndRemoveExtraSetReordersSessionPlan() = runTest {
        val dao = FakeSessionDao()
        val repository = SessionRepositoryImpl(dao)
        val sessionId = repository.startSession(
            workoutId = 1L,
            plannedSets = listOf(
                SessionPlannedSet(
                    sessionId = 0,
                    workoutSlotId = 11,
                    setOrder = 0,
                    exerciseId = 1006,
                    setType = SessionSetType.WORK,
                    targetReps = 5,
                    targetWeightKg = 100.0,
                ),
                SessionPlannedSet(
                    sessionId = 0,
                    workoutSlotId = 11,
                    setOrder = 1,
                    exerciseId = 1006,
                    setType = SessionSetType.WORK,
                    targetReps = 5,
                    targetWeightKg = 100.0,
                ),
                SessionPlannedSet(
                    sessionId = 0,
                    workoutSlotId = 12,
                    setOrder = 2,
                    exerciseId = 1007,
                    setType = SessionSetType.WORK,
                    targetReps = 8,
                    targetWeightKg = 40.0,
                ),
            ),
        )
        val original = repository.getActiveSessionState(sessionId)!!.plannedSets

        val withExtra = repository.addExtraSet(sessionId, original.first().id)!!.plannedSets
        assertEquals(listOf(0, 1, 2, 3), withExtra.map { it.setOrder })
        assertTrue(withExtra[1].isExtra)
        assertEquals(original.first().workoutSlotId, withExtra[1].workoutSlotId)
        assertEquals(original.first().setType, withExtra[1].setType)

        val withoutExtra = repository.removeExtraSet(sessionId, withExtra[1].id)!!.plannedSets
        assertEquals(listOf(0, 1, 2), withoutExtra.map { it.setOrder })
        assertTrue(withoutExtra.none { it.isExtra })
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

    override suspend fun getLatestUnfinishedSessionId(): Long? =
        sessions.values
            .filter { it.endedAtEpochMs == null }
            .maxByOrNull { it.startedAtEpochMs }
            ?.id

    override suspend fun getPlannedSets(sessionId: Long): List<SessionPlannedSetEntity> =
        plannedSetsBySession[sessionId].orEmpty().sortedBy { it.setOrder }

    override suspend fun getPlannedSet(sessionId: Long, plannedSetId: Long): SessionPlannedSetEntity? =
        plannedSetsBySession[sessionId].orEmpty().firstOrNull { it.id == plannedSetId }

    override suspend fun updatePlannedSetCompletion(
        sessionId: Long,
        plannedSetId: Long,
        isCompleted: Boolean,
        completedReps: Int?,
    ) {
        val current = plannedSetsBySession[sessionId] ?: return
        val idx = current.indexOfFirst { it.id == plannedSetId }
        if (idx >= 0) {
            current[idx] = current[idx].copy(isCompleted = isCompleted, completedReps = completedReps)
        }
    }

    override suspend fun updatePlannedSetWeight(sessionId: Long, plannedSetId: Long, weightKg: Double) {
        val current = plannedSetsBySession[sessionId] ?: return
        val idx = current.indexOfFirst { it.id == plannedSetId }
        if (idx >= 0) {
            current[idx] = current[idx].copy(targetWeightKg = weightKg)
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

    override suspend fun getExerciseHistoryRows(exerciseId: Long): List<ExerciseHistoryRow> = emptyList()

    override suspend fun getAllExerciseHistoryRows(): List<ExerciseHistoryRow> = emptyList()

    override suspend fun getSetResultForPlannedSet(sessionId: Long, plannedSetId: Long): SetResultEntity? =
        setResultsBySession[sessionId].orEmpty().firstOrNull { it.plannedSetId == plannedSetId }

    override suspend fun deleteSetResultForPlannedSet(sessionId: Long, plannedSetId: Long) {
        val current = setResultsBySession[sessionId] ?: return
        current.removeAll { it.plannedSetId == plannedSetId }
    }

    override suspend fun deletePlannedSetsForSession(sessionId: Long) {
        plannedSetsBySession.remove(sessionId)
    }

    override suspend fun deleteSetResultsForSession(sessionId: Long) {
        setResultsBySession.remove(sessionId)
    }

    override suspend fun deleteSession(sessionId: Long) {
        sessions.remove(sessionId)
    }

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
