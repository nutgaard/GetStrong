package no.utgdev.getstrong.ui.home

data class HomeUiState(
    val isRunningDemo: Boolean = false,
    val isLoadingCatalog: Boolean = false,
    val catalogCount: Int = 0,
    val catalogPreview: List<String> = emptyList(),
    val demoResultMessage: String = "Tap 'Run Persistence Demo' to verify local storage.",
)
