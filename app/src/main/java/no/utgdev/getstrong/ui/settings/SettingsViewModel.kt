package no.utgdev.getstrong.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.utgdev.getstrong.domain.model.ProgressionModeCode
import no.utgdev.getstrong.domain.repository.SettingsRepository

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            _uiState.update {
                it.copy(
                    isLoaded = true,
                    restDurationInput = settings.restDurationSeconds.toString(),
                    incrementInput = settings.loadIncrementKg.toString(),
                    deloadInput = settings.deloadPercent.toString(),
                    progressionMode = settings.defaultProgressionMode,
                    feedbackMessage = "",
                    hasError = false,
                )
            }
        }
    }

    fun updateRestDurationInput(value: String) {
        _uiState.update { it.copy(restDurationInput = value, feedbackMessage = "", hasError = false) }
    }

    fun updateIncrementInput(value: String) {
        _uiState.update { it.copy(incrementInput = value, feedbackMessage = "", hasError = false) }
    }

    fun updateDeloadInput(value: String) {
        _uiState.update { it.copy(deloadInput = value, feedbackMessage = "", hasError = false) }
    }

    fun updateProgressionMode(mode: String) {
        _uiState.update { it.copy(progressionMode = mode, feedbackMessage = "", hasError = false) }
    }

    fun save() {
        viewModelScope.launch {
            val current = _uiState.value
            val restSeconds = current.restDurationInput.toIntOrNull()
            val increment = current.incrementInput.toDoubleOrNull()
            val deloadPercent = current.deloadInput.toIntOrNull()
            val progressionMode = current.progressionMode

            val validationError = when {
                restSeconds == null || restSeconds < 1 -> "Rest duration must be at least 1 second."
                increment == null || increment <= 0.0 -> "Increment must be a positive number."
                deloadPercent == null || deloadPercent !in 1..99 -> "Deload percent must be 1-99."
                progressionMode !in setOf(
                    ProgressionModeCode.WEIGHT_ONLY,
                    ProgressionModeCode.REPS_ONLY,
                    ProgressionModeCode.REPS_THEN_WEIGHT,
                ) -> "Select a valid progression mode."
                else -> null
            }

            if (validationError != null) {
                _uiState.update { it.copy(feedbackMessage = validationError, hasError = true) }
                return@launch
            }

            val safeRestSeconds = restSeconds ?: return@launch
            val safeIncrement = increment ?: return@launch
            val safeDeloadPercent = deloadPercent ?: return@launch
            settingsRepository.updateDefaults(
                restDurationSeconds = safeRestSeconds,
                loadIncrementKg = safeIncrement,
                deloadPercent = safeDeloadPercent,
                defaultProgressionMode = progressionMode,
            )
            _uiState.update { it.copy(feedbackMessage = "Settings saved", hasError = false) }
        }
    }
}
