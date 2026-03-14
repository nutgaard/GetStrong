package no.utgdev.getstrong.domain.usecase

import javax.inject.Inject
import no.utgdev.getstrong.domain.model.WorkoutSummary
import no.utgdev.getstrong.domain.repository.SessionRepository
import no.utgdev.getstrong.domain.repository.WorkoutRepository
import no.utgdev.getstrong.domain.repository.SessionSummaryRepository
import no.utgdev.getstrong.domain.repository.WorkoutSummaryRepository

class CompleteSessionAndSaveSummaryUseCase @Inject constructor(
    private val completeSessionWithProgressionUseCase: CompleteSessionWithProgressionUseCase,
    private val sessionRepository: SessionRepository,
    private val sessionSummaryRepository: SessionSummaryRepository,
    private val workoutSummaryRepository: WorkoutSummaryRepository,
    private val workoutRepository: WorkoutRepository,
) {
    suspend operator fun invoke(sessionId: Long): Long {
        val updates = completeSessionWithProgressionUseCase.calculateUpdates(sessionId) ?: return sessionId
        val persistedAtomically = sessionRepository.completeSessionWithProgressionAndPersistSummary(
            sessionId = sessionId,
            updates = updates,
        )

        // Fallback exists for non-production repositories that do not implement
        // atomic summary persistence in the completion boundary.
        if (!persistedAtomically) {
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
        }
        return sessionId
    }
}
