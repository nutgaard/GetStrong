package no.utgdev.getstrong.domain.model

data class AppSettings(
    val restDurationSeconds: Int,
    val loadIncrementKg: Double,
    val deloadPercent: Int,
)
