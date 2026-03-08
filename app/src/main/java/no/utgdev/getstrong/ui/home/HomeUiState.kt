package no.utgdev.getstrong.ui.home

data class HomeUiState(
    val isRunningDemo: Boolean = false,
    val demoResultMessage: String = "Tap 'Run Persistence Demo' to verify local storage.",
)
