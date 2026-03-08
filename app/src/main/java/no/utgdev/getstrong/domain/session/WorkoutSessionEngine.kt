package no.utgdev.getstrong.domain.session

import javax.inject.Inject
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.SessionSetType
import no.utgdev.getstrong.domain.model.Workout

class WorkoutSessionEngine @Inject constructor() {
    fun buildSessionPlan(workout: Workout): List<SessionPlannedSet> {
        val sortedSlots = workout.slots.sortedBy { it.position }
        val plannedSets = mutableListOf<SessionPlannedSet>()
        var setOrder = 0

        sortedSlots.forEach { slot ->
            plannedSets += SessionPlannedSet(
                setOrder = setOrder++,
                sessionId = 0,
                workoutSlotId = slot.id,
                exerciseId = slot.exerciseId,
                setType = SessionSetType.WARMUP,
                targetReps = warmupRepsFor(slot.targetReps),
            )

            repeat(slot.targetSets) {
                plannedSets += SessionPlannedSet(
                    setOrder = setOrder++,
                    sessionId = 0,
                    workoutSlotId = slot.id,
                    exerciseId = slot.exerciseId,
                    setType = SessionSetType.WORK,
                    targetReps = slot.targetReps,
                )
            }
        }

        return plannedSets
    }

    // Warmup generation is intentionally minimal for T7.
    private fun warmupRepsFor(targetReps: Int): Int = maxOf(3, targetReps / 2)
}
