package no.utgdev.getstrong.ui.summary

data class SummaryUiState(
    val sessionId: Long = 0L,
    val isLoading: Boolean = true,
    val totalVolumeKg: Double = 0.0,
    val totalDurationSeconds: Long = 0L,
    val volumeRule: String = "",
    val sets: List<SummarySetRowUi> = emptyList(),
)

data class SummarySetRowUi(
    val setOrder: Int,
    val setType: String,
    val exerciseId: Long,
    val targetReps: Int,
    val achievedReps: Int?,
    val loadKg: Double?,
)
