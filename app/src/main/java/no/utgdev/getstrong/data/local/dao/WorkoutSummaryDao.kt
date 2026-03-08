package no.utgdev.getstrong.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import no.utgdev.getstrong.data.local.entity.WorkoutSummaryEntity

@Dao
interface WorkoutSummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummary(summary: WorkoutSummaryEntity): Long

    @Query("SELECT * FROM workout_summaries ORDER BY completedAtEpochMs DESC")
    suspend fun getAllSummaries(): List<WorkoutSummaryEntity>

    @Query("SELECT * FROM workout_summaries WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSummaryBySessionId(sessionId: Long): WorkoutSummaryEntity?
}
