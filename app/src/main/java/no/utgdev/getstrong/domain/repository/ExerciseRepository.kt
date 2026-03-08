package no.utgdev.getstrong.domain.repository

import no.utgdev.getstrong.domain.model.Exercise

interface ExerciseRepository {
    suspend fun save(exercise: Exercise): Long
    suspend fun getById(exerciseId: Long): Exercise?
    suspend fun getAll(): List<Exercise>
    suspend fun delete(exerciseId: Long)
}
