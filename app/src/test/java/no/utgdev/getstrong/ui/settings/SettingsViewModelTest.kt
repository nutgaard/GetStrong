package no.utgdev.getstrong.ui.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import no.utgdev.getstrong.domain.model.AppSettings
import no.utgdev.getstrong.domain.model.ProgressionModeCode
import no.utgdev.getstrong.domain.repository.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

class SettingsViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun savesValidSettingsAndShowsFeedback() = runTest {
        val repo = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repo)
        advanceUntilIdle()

        viewModel.updateRestDurationInput("240")
        viewModel.updateIncrementInput("1.25")
        viewModel.updateDeloadInput("15")
        viewModel.updateProgressionMode(ProgressionModeCode.REPS_ONLY)
        viewModel.save()
        advanceUntilIdle()

        val settings = repo.settingsState.value
        assertEquals(240, settings.restDurationSeconds)
        assertEquals(1.25, settings.loadIncrementKg, 0.0)
        assertEquals(15, settings.deloadPercent)
        assertEquals(ProgressionModeCode.REPS_ONLY, settings.defaultProgressionMode)
        assertEquals("Settings saved", viewModel.uiState.value.feedbackMessage)
    }

    @Test
    fun rejectsInvalidSettingsInputs() = runTest {
        val repo = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repo)
        advanceUntilIdle()

        viewModel.updateRestDurationInput("0")
        viewModel.updateIncrementInput("-1")
        viewModel.updateDeloadInput("101")
        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasError)
        assertTrue(viewModel.uiState.value.feedbackMessage.isNotBlank())
    }
}

private class FakeSettingsRepository : SettingsRepository {
    val settingsState = MutableStateFlow(
        AppSettings(
            restDurationSeconds = 180,
            loadIncrementKg = 2.5,
            deloadPercent = 10,
            defaultProgressionMode = ProgressionModeCode.WEIGHT_ONLY,
        ),
    )

    override val settings: Flow<AppSettings> = settingsState

    override suspend fun updateDefaults(
        restDurationSeconds: Int,
        loadIncrementKg: Double,
        deloadPercent: Int,
        defaultProgressionMode: String,
    ) {
        settingsState.value =
            settingsState.value.copy(
                restDurationSeconds = restDurationSeconds,
                loadIncrementKg = loadIncrementKg,
                deloadPercent = deloadPercent,
                defaultProgressionMode = defaultProgressionMode,
            )
    }

    override suspend fun updateTrainingDays(trainingDays: List<Int>) {
        settingsState.value = settingsState.value.copy(trainingDays = trainingDays)
    }
}
