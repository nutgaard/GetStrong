package no.utgdev.getstrong.data.repository

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import no.utgdev.getstrong.data.local.settings.SettingsStore
import no.utgdev.getstrong.domain.model.AppSettings
import no.utgdev.getstrong.domain.repository.SettingsRepository

class SettingsRepositoryImpl @Inject constructor(
    private val settingsStore: SettingsStore,
) : SettingsRepository {
    override val settings: Flow<AppSettings> = settingsStore.settings

    override suspend fun updateDefaults(
        restDurationSeconds: Int,
        loadIncrementKg: Double,
        deloadPercent: Int,
    ) {
        settingsStore.updateDefaults(
            restDurationSeconds = restDurationSeconds,
            loadIncrementKg = loadIncrementKg,
            deloadPercent = deloadPercent,
        )
    }
}
