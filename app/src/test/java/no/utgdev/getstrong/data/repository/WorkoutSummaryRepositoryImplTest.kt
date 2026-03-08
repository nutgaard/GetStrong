package no.utgdev.getstrong.data.repository

import kotlinx.coroutines.test.runTest
import no.utgdev.getstrong.data.local.dao.WorkoutSummaryDao
import no.utgdev.getstrong.data.local.entity.WorkoutSummaryEntity
import no.utgdev.getstrong.domain.model.WorkoutSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WorkoutSummaryRepositoryImplTest {
    @Test
    fun saveSummaryIsIdempotentPerSessionAndHistoryIsReadable() = runTest {
        val dao = FakeWorkoutSummaryDao()
        val repository = WorkoutSummaryRepositoryImpl(dao)

        val sessionId = 99L
        repository.saveSummary(
            WorkoutSummary(
                workoutId = 1,
                sessionId = sessionId,
                workoutName = "A Workout",
                totalVolumeKg = 500.0,
                totalDurationSeconds = 1200,
                completedAtEpochMs = 1000L,
            ),
        )
        repository.saveSummary(
            WorkoutSummary(
                workoutId = 1,
                sessionId = sessionId,
                workoutName = "A Workout",
                totalVolumeKg = 550.0,
                totalDurationSeconds = 1300,
                completedAtEpochMs = 2000L,
            ),
        )

        val persisted = repository.getSummaryBySessionId(sessionId)
        val history = repository.getHistory()

        assertNotNull(persisted)
        assertEquals(1, dao.entities.size)
        assertEquals(1, history.size)
        assertEquals(550.0, persisted!!.totalVolumeKg, 0.0)
        assertEquals("A Workout", history[0].workoutName)
        assertEquals(sessionId, history[0].sessionId)
    }
}

private class FakeWorkoutSummaryDao : WorkoutSummaryDao {
    private var nextId = 1L
    val entities = linkedMapOf<Long, WorkoutSummaryEntity>()

    override suspend fun upsertSummary(summary: WorkoutSummaryEntity): Long {
        val id = if (summary.id == 0L) nextId++ else summary.id
        entities[id] = summary.copy(id = id)
        return id
    }

    override suspend fun getAllSummaries(): List<WorkoutSummaryEntity> =
        entities.values.sortedByDescending { it.completedAtEpochMs }

    override suspend fun getSummaryBySessionId(sessionId: Long): WorkoutSummaryEntity? =
        entities.values.firstOrNull { it.sessionId == sessionId }
}
