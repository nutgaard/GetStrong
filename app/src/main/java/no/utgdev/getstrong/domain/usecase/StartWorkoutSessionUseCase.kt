package no.utgdev.getstrong.domain.usecase

import javax.inject.Inject
import no.utgdev.getstrong.domain.repository.ExerciseRepository
import no.utgdev.getstrong.domain.repository.SessionRepository
import no.utgdev.getstrong.domain.repository.WorkoutRepository
import no.utgdev.getstrong.domain.session.WorkoutSessionEngine

class StartWorkoutSessionUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val sessionRepository: SessionRepository,
    private val sessionEngine: WorkoutSessionEngine,
) {
    suspend operator fun invoke(workoutId: Long): Long? {
        val workout = workoutRepository.getWorkout(workoutId) ?: return null
        val exercisesById = exerciseRepository.getAll().associateBy { it.id }
        val plan = sessionEngine.buildSessionPlan(workout, exercisesById)
        return sessionRepository.startSession(
            workoutId = workout.id,
            plannedSets = plan,
        )
    }
}
