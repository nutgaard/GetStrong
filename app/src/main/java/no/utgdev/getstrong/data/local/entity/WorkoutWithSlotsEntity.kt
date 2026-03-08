package no.utgdev.getstrong.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class WorkoutWithSlotsEntity(
    @Embedded val workout: WorkoutEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutId",
    )
    val slots: List<WorkoutExerciseSlotEntity>,
)
