package no.utgdev.getstrong.data.local.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
            defaultProgressionMode = prefs[DEFAULT_PROGRESSION_MODE_KEY] ?: SettingsDefaults.DEFAULT_PROGRESSION_MODE,
            trainingDays = parseTrainingDays(prefs[TRAINING_DAYS_KEY]),
        )
    }

    suspend fun updateDefaults(
        restDurationSeconds: Int,
        loadIncrementKg: Double,
        deloadPercent: Int,
        defaultProgressionMode: String,
    ) {
        dataStore.edit { prefs ->
            prefs[REST_DURATION_SECONDS_KEY] = restDurationSeconds
            prefs[LOAD_INCREMENT_KG_KEY] = loadIncrementKg
            prefs[DELOAD_PERCENT_KEY] = deloadPercent
            prefs[DEFAULT_PROGRESSION_MODE_KEY] = defaultProgressionMode
        }
    }

    suspend fun updateTrainingDays(trainingDays: List<Int>) {
        dataStore.edit { prefs ->
            prefs[TRAINING_DAYS_KEY] = normalizeTrainingDays(trainingDays).joinToString(",")
        }
    }

    private fun parseTrainingDays(raw: String?): List<Int> =
        normalizeTrainingDays(
            raw
                ?.split(",")
                ?.mapNotNull { token -> token.trim().toIntOrNull() }
                .orEmpty(),
        ).ifEmpty { SettingsDefaults.TRAINING_DAYS }

    private fun normalizeTrainingDays(trainingDays: List<Int>): List<Int> =
        trainingDays
            .filter { day -> day in 1..7 }
            .distinct()
            .sorted()

    private companion object {
        val REST_DURATION_SECONDS_KEY = intPreferencesKey("rest_duration_seconds")
        val LOAD_INCREMENT_KG_KEY = doublePreferencesKey("load_increment_kg")
        val DELOAD_PERCENT_KEY = intPreferencesKey("deload_percent")
        val DEFAULT_PROGRESSION_MODE_KEY = stringPreferencesKey("default_progression_mode")
        val TRAINING_DAYS_KEY = stringPreferencesKey("training_days")
    }
}
