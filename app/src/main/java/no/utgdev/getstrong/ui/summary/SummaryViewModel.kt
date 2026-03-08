package no.utgdev.getstrong.ui.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.utgdev.getstrong.domain.repository.SessionSummaryRepository
import no.utgdev.getstrong.ui.navigation.AppDestination

@HiltViewModel
class SummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionSummaryRepository: SessionSummaryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SummaryUiState())
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    private val sessionId =
        savedStateHandle.get<String>(AppDestination.Summary.SESSION_ID_ARG)?.toLongOrNull() ?: 0L

    init {
        loadSummary()
    }

    private fun loadSummary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, sessionId = sessionId) }
            val summary = sessionSummaryRepository.getSessionSummary(sessionId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    totalVolumeKg = summary?.totalVolumeKg ?: 0.0,
                    totalDurationSeconds = summary?.totalDurationSeconds ?: 0L,
                    volumeRule = summary?.volumeRule.orEmpty(),
                    sets = summary?.sets.orEmpty().map { row ->
                        SummarySetRowUi(
                            setOrder = row.setOrder,
                            setType = row.setType,
                            exerciseId = row.exerciseId,
                            targetReps = row.targetReps,
                            achievedReps = row.achievedReps,
                            loadKg = row.loadKg,
                        )
                    },
                )
            }
        }
    }
}
