package no.utgdev.getstrong.domain.model

data class Exercise(
    val id: Long = 0,
    val name: String,
    val primaryMuscleGroup: String,
    val secondaryMuscleGroups: List<String> = emptyList(),
    val equipmentType: String,
)
