package no.utgdev.getstrong.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import no.utgdev.getstrong.data.local.entity.SessionPlannedSetEntity
import no.utgdev.getstrong.data.local.entity.SetResultEntity
import no.utgdev.getstrong.data.local.entity.WorkoutSessionEntity

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: WorkoutSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSetResult(result: SetResultEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlannedSets(plannedSets: List<SessionPlannedSetEntity>)

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getSession(sessionId: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM session_planned_sets WHERE sessionId = :sessionId ORDER BY setOrder ASC")
    suspend fun getPlannedSets(sessionId: Long): List<SessionPlannedSetEntity>

    @Query("UPDATE session_planned_sets SET isCompleted = 1, completedReps = :completedReps WHERE id = :plannedSetId AND sessionId = :sessionId")
    suspend fun markPlannedSetCompleted(sessionId: Long, plannedSetId: Long, completedReps: Int)

    @Query("UPDATE sessions SET endedAtEpochMs = :endedAtEpochMs WHERE id = :sessionId")
    suspend fun markSessionCompleted(sessionId: Long, endedAtEpochMs: Long)

    @Query(
        """
        UPDATE workout_exercise_slots
        SET targetReps = :nextTargetReps,
            currentWorkingWeightKg = :nextWorkingWeightKg,
            lastProgressionSessionId = :sessionId
        WHERE id = :slotId
          AND (lastProgressionSessionId IS NULL OR lastProgressionSessionId != :sessionId)
        """,
    )
    suspend fun applySlotProgressionForSession(
        sessionId: Long,
        slotId: Long,
        nextTargetReps: Int,
        nextWorkingWeightKg: Double,
    )

    @Query("SELECT * FROM set_results WHERE sessionId = :sessionId ORDER BY id ASC")
    suspend fun getSetResults(sessionId: Long): List<SetResultEntity>

    @Transaction
    suspend fun createSessionWithPlan(
        session: WorkoutSessionEntity,
        plannedSets: List<SessionPlannedSetEntity>,
    ): Long {
        val sessionId = upsertSession(session)
        val mapped = plannedSets.map { it.copy(sessionId = sessionId) }
        insertPlannedSets(mapped)
        return sessionId
    }

    @Transaction
    suspend fun completeSessionWithProgression(
        sessionId: Long,
        endedAtEpochMs: Long,
        updates: List<SlotProgressionRecord>,
    ) {
        updates.forEach { update ->
            applySlotProgressionForSession(
                sessionId = sessionId,
                slotId = update.slotId,
                nextTargetReps = update.nextTargetReps,
                nextWorkingWeightKg = update.nextWorkingWeightKg,
            )
        }
        markSessionCompleted(sessionId, endedAtEpochMs)
    }
}

data class SlotProgressionRecord(
    val slotId: Long,
    val nextTargetReps: Int,
    val nextWorkingWeightKg: Double,
)
