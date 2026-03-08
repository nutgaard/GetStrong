package no.utgdev.getstrong.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.utgdev.getstrong.domain.repository.WorkoutSummaryRepository

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val workoutSummaryRepository: WorkoutSummaryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val history = workoutSummaryRepository.getHistory()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    items = history.map { item ->
                        HistoryItemUi(
                            id = item.id,
                            sessionId = item.sessionId,
                            workoutName = item.workoutName,
                            totalVolumeKg = item.totalVolumeKg,
                            totalDurationSeconds = item.totalDurationSeconds,
                            completedAtEpochMs = item.completedAtEpochMs,
                        )
                    },
                )
            }
        }
    }
}
