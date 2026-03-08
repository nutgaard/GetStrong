package no.utgdev.getstrong.domain.session

import no.utgdev.getstrong.domain.model.EquipmentTypeCode
import no.utgdev.getstrong.domain.model.Exercise
import no.utgdev.getstrong.domain.model.MuscleGroupCode
import no.utgdev.getstrong.domain.model.ProgressionModeCode
import no.utgdev.getstrong.domain.model.SessionSetType
import no.utgdev.getstrong.domain.model.Workout
import no.utgdev.getstrong.domain.model.WorkoutExerciseSlot
import no.utgdev.getstrong.domain.usecase.WarmupGenerator
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutSessionEngineTest {
    @Test
    fun buildSessionPlanPreservesExerciseOrderAndWarmupBeforeWork() {
        val workout = Workout(
            id = 1,
            name = "A/B",
            slots = listOf(
                WorkoutExerciseSlot(
                    id = 10,
                    workoutId = 1,
                    exerciseId = 1006,
                    position = 0,
                    targetSets = 2,
                    targetReps = 5,
                    repRangeMin = 5,
                    repRangeMax = 5,
                    progressionMode = ProgressionModeCode.WEIGHT_ONLY,
                    incrementKg = 2.5,
                    deloadPercent = 10,
                    currentWorkingWeightKg = 100.0,
                    failureStreak = 0,
                    restSecondsOverride = null,
                ),
                WorkoutExerciseSlot(
                    id = 11,
                    workoutId = 1,
                    exerciseId = 1036,
                    position = 1,
                    targetSets = 1,
                    targetReps = 8,
                    repRangeMin = 8,
                    repRangeMax = 8,
                    progressionMode = ProgressionModeCode.WEIGHT_ONLY,
                    incrementKg = 2.5,
                    deloadPercent = 10,
                    currentWorkingWeightKg = 70.0,
                    failureStreak = 0,
                    restSecondsOverride = null,
                ),
            ),
        )

        val plan = WorkoutSessionEngine(WarmupGenerator()).buildSessionPlan(
            workout = workout,
            exercisesById = mapOf(
                1006L to Exercise(
                    id = 1006,
                    name = "Squat",
                    primaryMuscleGroup = MuscleGroupCode.QUADS,
                    secondaryMuscleGroups = emptyList(),
                    equipmentType = EquipmentTypeCode.BARBELL,
                ),
                1036L to Exercise(
                    id = 1036,
                    name = "Row",
                    primaryMuscleGroup = MuscleGroupCode.BACK,
                    secondaryMuscleGroups = emptyList(),
                    equipmentType = EquipmentTypeCode.BARBELL,
                ),
            ),
        )

        assertEquals(7, plan.size)
        assertEquals(SessionSetType.WARMUP, plan[0].setType)
        assertEquals(10L, plan[0].workoutSlotId)
        assertEquals(1006, plan[0].exerciseId)
        assertEquals(SessionSetType.WARMUP, plan[1].setType)
        assertEquals(SessionSetType.WARMUP, plan[2].setType)
        assertEquals(SessionSetType.WORK, plan[3].setType)
        assertEquals(SessionSetType.WORK, plan[4].setType)

        assertEquals(SessionSetType.WARMUP, plan[5].setType)
        assertEquals(11L, plan[5].workoutSlotId)
        assertEquals(1036, plan[5].exerciseId)
        assertEquals(SessionSetType.WORK, plan[6].setType)

        assertEquals(listOf(45.0, 65.0, 80.0), plan.take(3).map { it.targetWeightKg })
        assertEquals(42.5, plan[5].targetWeightKg)

        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6), plan.map { it.setOrder })
    }
}
