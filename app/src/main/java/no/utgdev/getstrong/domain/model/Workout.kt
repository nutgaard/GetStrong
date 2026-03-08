package no.utgdev.getstrong.domain.model

data class Workout(
    val id: Long = 0,
    val name: String,
    val slots: List<WorkoutExerciseSlot>,
)

data class WorkoutExerciseSlot(
    val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    val position: Int,
    val targetSets: Int,
    val targetReps: Int,
    val repRangeMin: Int,
    val repRangeMax: Int,
    val progressionMode: String,
    val incrementKg: Double,
    val deloadPercent: Int,
    val restSecondsOverride: Int? = null,
)
