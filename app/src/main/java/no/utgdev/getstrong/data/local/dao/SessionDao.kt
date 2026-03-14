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

    @Query("SELECT id FROM sessions WHERE endedAtEpochMs IS NULL ORDER BY startedAtEpochMs DESC LIMIT 1")
    suspend fun getLatestUnfinishedSessionId(): Long?

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

    @Query(
        """
        SELECT
            set_results.exerciseId AS exerciseId,
            set_results.sessionId AS sessionId,
            COALESCE(workout_summaries.workoutName, '') AS workoutName,
            sessions.endedAtEpochMs AS completedAtEpochMs,
            set_results.reps AS reps,
            set_results.weightKg AS weightKg
        FROM set_results
        INNER JOIN sessions ON sessions.id = set_results.sessionId
        LEFT JOIN workout_summaries ON workout_summaries.sessionId = set_results.sessionId
        WHERE set_results.exerciseId = :exerciseId
          AND set_results.setType != 'WARMUP'
          AND sessions.endedAtEpochMs IS NOT NULL
        ORDER BY sessions.endedAtEpochMs DESC, set_results.id DESC
        """,
    )
    suspend fun getExerciseHistoryRows(exerciseId: Long): List<ExerciseHistoryRow>

    @Query(
        """
        SELECT
            set_results.exerciseId AS exerciseId,
            set_results.sessionId AS sessionId,
            COALESCE(workout_summaries.workoutName, '') AS workoutName,
            sessions.endedAtEpochMs AS completedAtEpochMs,
            set_results.reps AS reps,
            set_results.weightKg AS weightKg
        FROM set_results
        INNER JOIN sessions ON sessions.id = set_results.sessionId
        LEFT JOIN workout_summaries ON workout_summaries.sessionId = set_results.sessionId
        WHERE set_results.setType != 'WARMUP'
          AND sessions.endedAtEpochMs IS NOT NULL
        ORDER BY set_results.exerciseId ASC, sessions.endedAtEpochMs DESC, set_results.id DESC
        """,
    )
    suspend fun getAllExerciseHistoryRows(): List<ExerciseHistoryRow>

    @Query("SELECT * FROM set_results WHERE sessionId = :sessionId AND plannedSetId = :plannedSetId LIMIT 1")
    suspend fun getSetResultForPlannedSet(sessionId: Long, plannedSetId: Long): SetResultEntity?

    @Query("DELETE FROM set_results WHERE sessionId = :sessionId AND plannedSetId = :plannedSetId")
    suspend fun deleteSetResultForPlannedSet(sessionId: Long, plannedSetId: Long)

    @Query("DELETE FROM session_planned_sets WHERE sessionId = :sessionId")
    suspend fun deletePlannedSetsForSession(sessionId: Long)

    @Query("DELETE FROM set_results WHERE sessionId = :sessionId")
    suspend fun deleteSetResultsForSession(sessionId: Long)

    @Query("DELETE FROM workout_summaries WHERE sessionId = :sessionId")
    suspend fun deleteWorkoutSummaryForSession(sessionId: Long)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query(
        """
        INSERT INTO workout_summaries (
            workoutId,
            sessionId,
            workoutName,
            totalVolumeKg,
            totalDurationSeconds,
            completedAtEpochMs
        )
        SELECT
            sessions.workoutId,
            sessions.id,
            workouts.name,
            COALESCE(SUM(CASE WHEN set_results.setType = 'WORK' THEN set_results.reps * set_results.weightKg ELSE 0 END), 0),
            MAX((sessions.endedAtEpochMs - sessions.startedAtEpochMs) / 1000, 0),
            sessions.endedAtEpochMs
        FROM sessions
        INNER JOIN workouts ON workouts.id = sessions.workoutId
        LEFT JOIN set_results ON set_results.sessionId = sessions.id
        WHERE sessions.id = :sessionId
        GROUP BY sessions.id, sessions.workoutId, workouts.name, sessions.startedAtEpochMs, sessions.endedAtEpochMs
        """,
    )
    suspend fun insertWorkoutSummaryProjectionForSession(sessionId: Long): Long

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
    suspend fun discardSession(sessionId: Long) {
        deleteSetResultsForSession(sessionId)
        deletePlannedSetsForSession(sessionId)
        deleteSession(sessionId)
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

    @Transaction
    suspend fun completeSessionWithProgressionAndPersistSummary(
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
        deleteWorkoutSummaryForSession(sessionId)
        insertWorkoutSummaryProjectionForSession(sessionId)
    }
}

data class SlotProgressionRecord(
    val slotId: Long,
    val nextTargetReps: Int,
    val nextWorkingWeightKg: Double,
    val nextFailureStreak: Int,
)

data class ExerciseHistoryRow(
    val exerciseId: Long,
    val sessionId: Long,
    val workoutName: String,
    val completedAtEpochMs: Long,
    val reps: Int,
    val weightKg: Double,
)
