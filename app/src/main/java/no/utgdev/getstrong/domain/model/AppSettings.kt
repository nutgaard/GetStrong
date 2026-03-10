package no.utgdev.getstrong.domain.model

data class AppSettings(
    val restDurationSeconds: Int,
    val loadIncrementKg: Double,
    val deloadPercent: Int,
    val defaultProgressionMode: String,
    val trainingDays: List<Int> = listOf(1, 3, 5),
)
