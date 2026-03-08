package no.utgdev.getstrong.domain.usecase

import javax.inject.Inject
import no.utgdev.getstrong.domain.model.WorkoutSummary
import no.utgdev.getstrong.domain.repository.WorkoutRepository
import no.utgdev.getstrong.domain.repository.SessionSummaryRepository
import no.utgdev.getstrong.domain.repository.WorkoutSummaryRepository

class CompleteSessionAndSaveSummaryUseCase @Inject constructor(
    private val completeSessionWithProgressionUseCase: CompleteSessionWithProgressionUseCase,
    private val sessionSummaryRepository: SessionSummaryRepository,
    private val workoutSummaryRepository: WorkoutSummaryRepository,
    private val workoutRepository: WorkoutRepository,
) {
    suspend operator fun invoke(sessionId: Long): Long {
        completeSessionWithProgressionUseCase(sessionId)
        val summary = sessionSummaryRepository.getSessionSummary(sessionId) ?: return sessionId
        val workoutName = workoutRepository.getWorkout(summary.workoutId)?.name ?: "Workout ${summary.workoutId}"
        workoutSummaryRepository.saveSummary(
            WorkoutSummary(
                workoutId = summary.workoutId,
                sessionId = summary.sessionId,
                workoutName = workoutName,
                totalVolumeKg = summary.totalVolumeKg,
                totalDurationSeconds = summary.totalDurationSeconds,
                completedAtEpochMs = System.currentTimeMillis(),
            ),
        )
        return sessionId
    }
}
