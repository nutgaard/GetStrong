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

    @Query("SELECT * FROM session_planned_sets WHERE id = :plannedSetId AND sessionId = :sessionId LIMIT 1")
    suspend fun getPlannedSet(sessionId: Long, plannedSetId: Long): SessionPlannedSetEntity?

    @Query(
        """
        UPDATE session_planned_sets
        SET isCompleted = :isCompleted,
            completedReps = :completedReps
        WHERE id = :plannedSetId AND sessionId = :sessionId
        """,
    )
    suspend fun updatePlannedSetCompletion(
        sessionId: Long,
        plannedSetId: Long,
        isCompleted: Boolean,
        completedReps: Int?,
    )

    @Query(
        """
        UPDATE session_planned_sets
        SET targetWeightKg = :weightKg
        WHERE id = :plannedSetId AND sessionId = :sessionId
        """,
    )
    suspend fun updatePlannedSetWeight(sessionId: Long, plannedSetId: Long, weightKg: Double)

    @Query("UPDATE sessions SET endedAtEpochMs = :endedAtEpochMs WHERE id = :sessionId")
    suspend fun markSessionCompleted(sessionId: Long, endedAtEpochMs: Long)

    @Query(
        """
        UPDATE workout_exercise_slots
        SET targetReps = :nextTargetReps,
            currentWorkingWeightKg = :nextWorkingWeightKg,
            failureStreak = :nextFailureStreak,
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
        nextFailureStreak: Int,
    )

    @Query("SELECT * FROM set_results WHERE sessionId = :sessionId ORDER BY id ASC")
    suspend fun getSetResults(sessionId: Long): List<SetResultEntity>

    @Query("SELECT * FROM set_results WHERE sessionId = :sessionId AND plannedSetId = :plannedSetId LIMIT 1")
    suspend fun getSetResultForPlannedSet(sessionId: Long, plannedSetId: Long): SetResultEntity?

    @Query("DELETE FROM set_results WHERE sessionId = :sessionId AND plannedSetId = :plannedSetId")
    suspend fun deleteSetResultForPlannedSet(sessionId: Long, plannedSetId: Long)

    @Query("DELETE FROM session_planned_sets WHERE sessionId = :sessionId")
    suspend fun deletePlannedSetsForSession(sessionId: Long)

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
    suspend fun replaceSessionPlan(
        sessionId: Long,
        plannedSets: List<SessionPlannedSetEntity>,
    ) {
        deletePlannedSetsForSession(sessionId)
        insertPlannedSets(plannedSets.map { it.copy(sessionId = sessionId) })
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
                nextFailureStreak = update.nextFailureStreak,
            )
        }
        markSessionCompleted(sessionId, endedAtEpochMs)
    }
}

data class SlotProgressionRecord(
    val slotId: Long,
    val nextTargetReps: Int,
    val nextWorkingWeightKg: Double,
    val nextFailureStreak: Int,
)
