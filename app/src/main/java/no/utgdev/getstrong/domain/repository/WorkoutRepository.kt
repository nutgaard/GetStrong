package no.utgdev.getstrong.domain.repository

import no.utgdev.getstrong.domain.model.Workout

interface WorkoutRepository {
    suspend fun createWorkout(workout: Workout): Long
    suspend fun updateWorkout(workout: Workout)
    suspend fun deleteWorkout(workoutId: Long)
    suspend fun getWorkout(workoutId: Long): Workout?
    suspend fun getAllWorkouts(): List<Workout>
}
