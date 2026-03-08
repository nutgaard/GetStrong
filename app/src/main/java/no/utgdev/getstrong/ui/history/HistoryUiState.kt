package no.utgdev.getstrong.ui.history

data class HistoryUiState(
    val isLoading: Boolean = true,
    val items: List<HistoryItemUi> = emptyList(),
)

data class HistoryItemUi(
    val id: Long,
    val sessionId: Long,
    val workoutName: String,
    val totalVolumeKg: Double,
    val totalDurationSeconds: Long,
    val completedAtEpochMs: Long,
)
