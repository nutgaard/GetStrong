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
                    result.exerciseId == slot.exerciseId && result.setType == SessionSetType.WORK
                }
                .map { it.reps }
                .toList()

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

            if (result.nextTargetReps == slot.targetReps && result.nextWorkingWeightKg == slot.currentWorkingWeightKg) {
                SlotProgressionUpdate(
                    slotId = slot.id,
                    nextTargetReps = slot.targetReps,
                    nextWorkingWeightKg = slot.currentWorkingWeightKg,
                )
            } else {
                SlotProgressionUpdate(
                    slotId = slot.id,
                    nextTargetReps = result.nextTargetReps,
                    nextWorkingWeightKg = result.nextWorkingWeightKg,
                )
            }
        }

        sessionRepository.completeSessionWithProgression(
            sessionId = sessionId,
            updates = updates,
        )
    }
}
