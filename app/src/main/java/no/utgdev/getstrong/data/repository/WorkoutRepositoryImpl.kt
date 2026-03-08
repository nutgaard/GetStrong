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
    override suspend fun createWorkout(workout: Workout): Long =
        workoutDao.saveWorkoutWithSlots(
            workout = WorkoutEntity(id = 0, name = workout.name),
            slots = workout.slots.map { it.toEntity(workoutId = 0) },
        )

    override suspend fun updateWorkout(workout: Workout) {
        require(workout.id > 0) { "Workout id must be set when updating" }
        workoutDao.saveWorkoutWithSlots(
            workout = WorkoutEntity(id = workout.id, name = workout.name),
            slots = workout.slots.map { it.toEntity(workoutId = workout.id) },
        )
    }

    override suspend fun deleteWorkout(workoutId: Long) {
        workoutDao.deleteWorkout(workoutId)
    }

    override suspend fun getWorkout(workoutId: Long): Workout? =
        workoutDao.getWorkoutWithSlots(workoutId)?.toDomain()

    override suspend fun getAllWorkouts(): List<Workout> =
        workoutDao.getAllWorkoutsWithSlots().map { it.toDomain() }
}

private fun WorkoutExerciseSlot.toEntity(workoutId: Long): WorkoutExerciseSlotEntity =
    WorkoutExerciseSlotEntity(
        id = id,
        workoutId = workoutId,
        exerciseId = exerciseId,
        position = position,
        targetSets = targetSets,
        targetReps = targetReps,
        repRangeMin = repRangeMin,
        repRangeMax = repRangeMax,
        progressionMode = progressionMode,
        incrementKg = incrementKg,
        deloadPercent = deloadPercent,
        currentWorkingWeightKg = currentWorkingWeightKg,
        lastProgressionSessionId = lastProgressionSessionId,
        restSecondsOverride = restSecondsOverride,
    )

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
                targetSets = it.targetSets,
                targetReps = it.targetReps,
                repRangeMin = it.repRangeMin,
                repRangeMax = it.repRangeMax,
                progressionMode = it.progressionMode,
                incrementKg = it.incrementKg,
                deloadPercent = it.deloadPercent,
                currentWorkingWeightKg = it.currentWorkingWeightKg,
                lastProgressionSessionId = it.lastProgressionSessionId,
                restSecondsOverride = it.restSecondsOverride,
            )
        },
    )
