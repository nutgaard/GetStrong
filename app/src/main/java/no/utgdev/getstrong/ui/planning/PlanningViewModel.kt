package no.utgdev.getstrong.ui.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.utgdev.getstrong.domain.repository.WorkoutRepository

@HiltViewModel
class PlanningViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlanningUiState())
    val uiState: StateFlow<PlanningUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val workouts = workoutRepository.getAllWorkouts()
            _uiState.update { it.copy(workouts = workouts) }
        }
    }

    fun deleteWorkout(workoutId: Long) {
        viewModelScope.launch {
            workoutRepository.deleteWorkout(workoutId)
            refresh()
        }
    }
}
