package no.utgdev.getstrong.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import no.utgdev.getstrong.data.local.entity.WorkoutEntity
import no.utgdev.getstrong.data.local.entity.WorkoutExerciseSlotEntity
import no.utgdev.getstrong.data.local.entity.WorkoutWithSlotsEntity

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkout(workout: WorkoutEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(slots: List<WorkoutExerciseSlotEntity>)

    @Query("DELETE FROM workout_exercise_slots WHERE workoutId = :workoutId")
    suspend fun deleteSlotsForWorkout(workoutId: Long)

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    suspend fun getWorkoutWithSlots(workoutId: Long): WorkoutWithSlotsEntity?

    @Transaction
    @Query("SELECT * FROM workouts ORDER BY name ASC")
    suspend fun getAllWorkoutsWithSlots(): List<WorkoutWithSlotsEntity>
}
