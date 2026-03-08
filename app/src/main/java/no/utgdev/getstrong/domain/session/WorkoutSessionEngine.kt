package no.utgdev.getstrong.domain.session

import javax.inject.Inject
import no.utgdev.getstrong.domain.model.Exercise
import no.utgdev.getstrong.domain.model.EquipmentTypeCode
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.SessionSetType
import no.utgdev.getstrong.domain.model.Workout
import no.utgdev.getstrong.domain.usecase.WarmupGenerator

class WorkoutSessionEngine @Inject constructor(
    private val warmupGenerator: WarmupGenerator,
) {
    fun buildSessionPlan(
        workout: Workout,
        exercisesById: Map<Long, Exercise>,
    ): List<SessionPlannedSet> {
        val sortedSlots = workout.slots.sortedBy { it.position }
        val plannedSets = mutableListOf<SessionPlannedSet>()
        var setOrder = 0

        sortedSlots.forEach { slot ->
            val equipment = exercisesById[slot.exerciseId]?.equipmentType ?: EquipmentTypeCode.BARBELL
            val warmups = warmupGenerator.generate(
                workingWeightKg = slot.currentWorkingWeightKg,
                equipmentType = equipment,
            )
            warmups.forEach { warmupWeight ->
                plannedSets += SessionPlannedSet(
                    setOrder = setOrder++,
                    sessionId = 0,
                    workoutSlotId = slot.id,
                    exerciseId = slot.exerciseId,
                    setType = SessionSetType.WARMUP,
                    targetReps = warmupRepsFor(slot.targetReps),
                    targetWeightKg = warmupWeight,
                )
            }

            repeat(slot.targetSets) {
                plannedSets += SessionPlannedSet(
                    setOrder = setOrder++,
                    sessionId = 0,
                    workoutSlotId = slot.id,
                    exerciseId = slot.exerciseId,
                    setType = SessionSetType.WORK,
                    targetReps = slot.targetReps,
                    targetWeightKg = slot.currentWorkingWeightKg,
                )
            }
        }

        return plannedSets
    }

    // Warmup generation is intentionally minimal for T7.
    private fun warmupRepsFor(targetReps: Int): Int = maxOf(3, targetReps / 2)
}
