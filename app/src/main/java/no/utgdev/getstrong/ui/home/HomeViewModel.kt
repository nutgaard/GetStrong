package no.utgdev.getstrong.ui.home

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
import no.utgdev.getstrong.domain.model.Exercise
import no.utgdev.getstrong.domain.model.EquipmentTypeCode
import no.utgdev.getstrong.domain.model.MuscleGroupCode
import no.utgdev.getstrong.domain.model.SetResult
import no.utgdev.getstrong.domain.model.Workout
import no.utgdev.getstrong.domain.model.WorkoutExerciseSlot
import no.utgdev.getstrong.domain.model.WorkoutSession
import no.utgdev.getstrong.domain.model.WorkoutSummary
import no.utgdev.getstrong.domain.model.ProgressionModeCode
import no.utgdev.getstrong.domain.repository.ExerciseRepository
import no.utgdev.getstrong.domain.repository.SessionRepository
import no.utgdev.getstrong.domain.repository.SettingsRepository
import no.utgdev.getstrong.domain.repository.WorkoutRepository
import no.utgdev.getstrong.domain.repository.WorkoutSummaryRepository

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val sessionRepository: SessionRepository,
    private val workoutSummaryRepository: WorkoutSummaryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadCatalog()
    }

    fun runPersistenceDemo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRunningDemo = true) }

            val exerciseId = exerciseRepository.save(
                Exercise(
                    name = "Barbell Row",
                    primaryMuscleGroup = MuscleGroupCode.BACK,
                    secondaryMuscleGroups = listOf(MuscleGroupCode.BICEPS, MuscleGroupCode.REAR_DELTS),
                    equipmentType = EquipmentTypeCode.BARBELL,
                ),
            )

            val workoutId = workoutRepository.createWorkout(
                Workout(
                    name = "Demo Pull Workout",
                    slots = listOf(
                        WorkoutExerciseSlot(
                            workoutId = 0,
                            exerciseId = exerciseId,
                            position = 0,
                            targetSets = 5,
                            targetReps = 5,
                            repRangeMin = 5,
                            repRangeMax = 5,
                            progressionMode = ProgressionModeCode.WEIGHT_ONLY,
                            incrementKg = 2.5,
                            deloadPercent = 10,
                            currentWorkingWeightKg = 0.0,
                            lastProgressionSessionId = null,
                            restSecondsOverride = null,
                        ),
                    ),
                ),
            )

            val startedAt = System.currentTimeMillis()
            val sessionId = sessionRepository.saveSession(
                WorkoutSession(
                    workoutId = workoutId,
                    startedAtEpochMs = startedAt,
                    endedAtEpochMs = startedAt + 120000,
                ),
            )

            sessionRepository.saveSetResult(
                SetResult(
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    setType = "work",
                    reps = 5,
                    weightKg = 60.0,
                ),
            )

            workoutSummaryRepository.saveSummary(
                WorkoutSummary(
                    workoutId = workoutId,
                    sessionId = sessionId,
                    totalVolumeKg = 300.0,
                    totalDurationSeconds = 120,
                    completedAtEpochMs = System.currentTimeMillis(),
                ),
            )

            settingsRepository.updateDefaults(
                restDurationSeconds = 180,
                loadIncrementKg = 2.5,
                deloadPercent = 10,
            )

            val loadedExerciseCount = exerciseRepository.getAll().size
            val loadedExercise = exerciseRepository.getById(exerciseId)
            val loadedWorkout = workoutRepository.getWorkout(workoutId)
            val loadedSetResults = sessionRepository.getSetResults(sessionId).size
            val loadedSummaries = workoutSummaryRepository.getAllSummaries().size
            val settings = settingsRepository.settings.first()

            _uiState.update {
                it.copy(
                    isRunningDemo = false,
                    demoResultMessage =
                        "Saved+loaded demo data: exercises=$loadedExerciseCount, " +
                            "exercise='${loadedExercise?.name}', secondary=${loadedExercise?.secondaryMuscleGroups?.size ?: 0}, " +
                            "workout='${loadedWorkout?.name}', setResults=$loadedSetResults, " +
                            "summaries=$loadedSummaries, rest=${settings.restDurationSeconds}s",
                )
            }
            loadCatalog()
        }
    }

    fun loadCatalog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCatalog = true) }
            val catalog = exerciseRepository.getAll()
            _uiState.update {
                it.copy(
                    isLoadingCatalog = false,
                    catalogCount = catalog.size,
                    catalogPreview = catalog.take(10).map { exercise ->
                        "${exercise.id}: ${exercise.name} (${exercise.primaryMuscleGroup})"
                    },
                )
            }
        }
    }
}
