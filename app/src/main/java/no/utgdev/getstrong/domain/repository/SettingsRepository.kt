package no.utgdev.getstrong.domain.repository

import kotlinx.coroutines.flow.Flow
import no.utgdev.getstrong.domain.model.AppSettings

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun updateDefaults(
        restDurationSeconds: Int,
        loadIncrementKg: Double,
        deloadPercent: Int,
        defaultProgressionMode: String,
    )
    suspend fun updateTrainingDays(trainingDays: List<Int>)
}
