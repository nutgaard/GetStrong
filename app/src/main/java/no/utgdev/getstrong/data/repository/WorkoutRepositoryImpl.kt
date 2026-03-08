package no.utgdev.getstrong.data.repository

import javax.inject.Inject
import no.utgdev.getstrong.data.local.dao.WorkoutDao
import no.utgdev.getstrong.data.local.entity.WorkoutEntity
import no.utgdev.getstrong.data.local.entity.WorkoutExerciseSlotEntity
import no.utgdev.getstrong.data.local.entity.WorkoutWithSlotsEntity
import no.utgdev.getstrong.domain.model.Workout
import no.utgdev.getstrong.domain.model.WorkoutExerciseSlot
import no.utgdev.getstrong.domain.repository.WorkoutRepository

class WorkoutRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao,
) : WorkoutRepository {
    override suspend fun saveWorkout(workout: Workout): Long {
        val workoutId = workoutDao.upsertWorkout(WorkoutEntity(id = workout.id, name = workout.name))
        workoutDao.deleteSlotsForWorkout(workoutId)
        workoutDao.insertSlots(
            workout.slots.map {
                WorkoutExerciseSlotEntity(
                    id = 0,
                    workoutId = workoutId,
                    exerciseId = it.exerciseId,
                    position = it.position,
                )
            },
        )
        return workoutId
    }

    override suspend fun getWorkout(workoutId: Long): Workout? =
        workoutDao.getWorkoutWithSlots(workoutId)?.toDomain()

    override suspend fun getAllWorkouts(): List<Workout> =
        workoutDao.getAllWorkoutsWithSlots().map { it.toDomain() }
}

private fun WorkoutWithSlotsEntity.toDomain(): Workout =
    Workout(
        id = workout.id,
        name = workout.name,
        slots = slots.sortedBy { it.position }.map {
            WorkoutExerciseSlot(
                id = it.id,
                workoutId = it.workoutId,
                exerciseId = it.exerciseId,
                position = it.position,
            )
        },
    )
