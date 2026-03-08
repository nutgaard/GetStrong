package no.utgdev.getstrong.ui.settings

data class SettingsUiState(
    val isLoaded: Boolean = false,
    val restDurationInput: String = "",
    val incrementInput: String = "",
    val deloadInput: String = "",
    val progressionMode: String = "",
    val feedbackMessage: String = "",
    val hasError: Boolean = false,
)
