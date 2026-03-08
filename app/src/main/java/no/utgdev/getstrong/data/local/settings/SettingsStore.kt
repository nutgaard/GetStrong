package no.utgdev.getstrong.data.local.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import no.utgdev.getstrong.domain.model.AppSettings

@Singleton
class SettingsStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            restDurationSeconds = prefs[REST_DURATION_SECONDS_KEY] ?: SettingsDefaults.REST_DURATION_SECONDS,
            loadIncrementKg = prefs[LOAD_INCREMENT_KG_KEY] ?: SettingsDefaults.LOAD_INCREMENT_KG,
            deloadPercent = prefs[DELOAD_PERCENT_KEY] ?: SettingsDefaults.DELOAD_PERCENT,
        )
    }

    suspend fun updateDefaults(
        restDurationSeconds: Int,
        loadIncrementKg: Double,
        deloadPercent: Int,
    ) {
        dataStore.edit { prefs ->
            prefs[REST_DURATION_SECONDS_KEY] = restDurationSeconds
            prefs[LOAD_INCREMENT_KG_KEY] = loadIncrementKg
            prefs[DELOAD_PERCENT_KEY] = deloadPercent
        }
    }

    private companion object {
        val REST_DURATION_SECONDS_KEY = intPreferencesKey("rest_duration_seconds")
        val LOAD_INCREMENT_KG_KEY = doublePreferencesKey("load_increment_kg")
        val DELOAD_PERCENT_KEY = intPreferencesKey("deload_percent")
    }
}
