package no.utgdev.getstrong.data.repository

import javax.inject.Inject
import no.utgdev.getstrong.data.local.dao.ExerciseDao
import no.utgdev.getstrong.data.local.entity.ExerciseEntity
import no.utgdev.getstrong.domain.model.Exercise
import no.utgdev.getstrong.domain.repository.ExerciseRepository

class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao,
) : ExerciseRepository {
    override suspend fun save(exercise: Exercise): Long {
        return if (exercise.id == 0L) {
            exerciseDao.insert(exercise.toEntity())
        } else {
            exerciseDao.update(exercise.toEntity())
            exercise.id
        }
    }

    override suspend fun getById(exerciseId: Long): Exercise? =
        exerciseDao.getById(exerciseId)?.toDomain()

    override suspend fun getAll(): List<Exercise> =
        exerciseDao.getAll().map { it.toDomain() }

    override suspend fun delete(exerciseId: Long) {
        exerciseDao.getById(exerciseId)?.let { exerciseDao.delete(it) }
    }
}

private fun Exercise.toEntity(): ExerciseEntity =
    ExerciseEntity(
        id = id,
        name = name,
        primaryMuscleGroup = primaryMuscleGroup,
        secondaryMuscleGroups = secondaryMuscleGroups,
        equipmentType = equipmentType,
    )

private fun ExerciseEntity.toDomain(): Exercise =
    Exercise(
        id = id,
        name = name,
        primaryMuscleGroup = primaryMuscleGroup,
        secondaryMuscleGroups = secondaryMuscleGroups,
        equipmentType = equipmentType,
    )
