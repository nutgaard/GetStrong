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
    suspend fun upsertSlots(slots: List<WorkoutExerciseSlotEntity>)

    @Query("DELETE FROM workout_exercise_slots WHERE workoutId = :workoutId")
    suspend fun deleteSlotsForWorkout(workoutId: Long)

    @Query("DELETE FROM workout_exercise_slots WHERE workoutId = :workoutId AND id NOT IN (:slotIds)")
    suspend fun deleteSlotsForWorkoutExcept(workoutId: Long, slotIds: List<Long>)

    @Query("DELETE FROM workouts WHERE id = :workoutId")
    suspend fun deleteWorkout(workoutId: Long)

    @Transaction
    suspend fun saveWorkoutWithSlots(
        workout: WorkoutEntity,
        slots: List<WorkoutExerciseSlotEntity>,
    ): Long {
        val workoutId = upsertWorkout(workout)
        if (slots.isEmpty()) {
            deleteSlotsForWorkout(workoutId)
            return workoutId
        }

        val mappedSlots = slots.map {
            it.copy(workoutId = workoutId)
        }

        val existingSlotIds = mappedSlots.mapNotNull { slot ->
            slot.id.takeIf { id -> id > 0 }
        }

        if (existingSlotIds.isEmpty()) {
            deleteSlotsForWorkout(workoutId)
        } else {
            deleteSlotsForWorkoutExcept(workoutId, existingSlotIds)
        }

        upsertSlots(mappedSlots)

        return workoutId
    }

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    suspend fun getWorkoutWithSlots(workoutId: Long): WorkoutWithSlotsEntity?

    @Transaction
    @Query("SELECT * FROM workouts ORDER BY name ASC")
    suspend fun getAllWorkoutsWithSlots(): List<WorkoutWithSlotsEntity>
}
