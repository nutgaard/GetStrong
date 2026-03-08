package no.utgdev.getstrong.domain.usecase

import javax.inject.Inject
import no.utgdev.getstrong.domain.model.ProgressionInput
import no.utgdev.getstrong.domain.model.SessionSetType
import no.utgdev.getstrong.domain.model.SlotProgressionUpdate
import no.utgdev.getstrong.domain.repository.SessionRepository
import no.utgdev.getstrong.domain.repository.WorkoutRepository

class CompleteSessionWithProgressionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val workoutRepository: WorkoutRepository,
    private val progressionCalculator: ProgressionCalculator,
) {
    suspend operator fun invoke(sessionId: Long) {
        val sessionState = sessionRepository.getActiveSessionState(sessionId) ?: return
        val workout = workoutRepository.getWorkout(sessionState.session.workoutId) ?: return
        val setResults = sessionRepository.getSetResults(sessionId)

        val updates = workout.slots.mapNotNull { slot ->
            if (slot.lastProgressionSessionId == sessionId) {
                return@mapNotNull null
            }

            val completedWorkReps = setResults
                .asSequence()
                .filter { result ->
                    result.workoutSlotId == slot.id && result.setType == SessionSetType.WORK
                }
                .map { it.reps }
                .toList()

            val isFailure = completedWorkReps.any { reps -> reps < slot.targetReps }
            val streakAfterOutcome = if (isFailure) slot.failureStreak + 1 else 0
            val shouldDeload = isFailure && streakAfterOutcome >= 3

            val result = progressionCalculator.calculate(
                ProgressionInput(
                    progressionMode = slot.progressionMode,
                    repRangeMin = slot.repRangeMin,
                    repRangeMax = slot.repRangeMax,
                    incrementKg = slot.incrementKg,
                    deloadPercent = slot.deloadPercent,
                    currentTargetReps = slot.targetReps,
                    currentWorkingWeightKg = slot.currentWorkingWeightKg,
                    workSetTargetCount = slot.targetSets,
                    completedWorkSetReps = completedWorkReps,
                ),
            )

            val nextWeight = if (shouldDeload) {
                progressionCalculator.applyDeload(
                    currentWorkingWeightKg = slot.currentWorkingWeightKg,
                    deloadPercent = slot.deloadPercent,
                    incrementKg = slot.incrementKg,
                )
            } else {
                result.nextWorkingWeightKg
            }

            val nextFailureStreak = if (shouldDeload) 0 else streakAfterOutcome

            SlotProgressionUpdate(
                slotId = slot.id,
                nextTargetReps = result.nextTargetReps,
                nextWorkingWeightKg = nextWeight,
                nextFailureStreak = nextFailureStreak,
            )
        }

        sessionRepository.completeSessionWithProgression(
            sessionId = sessionId,
            updates = updates,
        )
    }
}
