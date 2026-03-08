package no.utgdev.getstrong.domain.repository

import no.utgdev.getstrong.domain.model.Workout

interface WorkoutRepository {
    suspend fun saveWorkout(workout: Workout): Long
    suspend fun getWorkout(workoutId: Long): Workout?
    suspend fun getAllWorkouts(): List<Workout>
}
