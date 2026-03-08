package no.utgdev.getstrong.ui.history

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import no.utgdev.getstrong.domain.model.WorkoutHistoryItem
import no.utgdev.getstrong.domain.model.WorkoutSummary
import no.utgdev.getstrong.domain.repository.WorkoutSummaryRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HistoryViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadShowsEmptyStateWhenNoHistoryExists() = runTest {
        val viewModel = HistoryViewModel(
            workoutSummaryRepository = FakeWorkoutSummaryRepository(historyItems = emptyList()),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(!state.isLoading)
        assertNull(state.errorMessage)
        assertTrue(state.items.isEmpty())
    }

    @Test
    fun loadShowsErrorStateWhenHistoryReadFails() = runTest {
        val viewModel = HistoryViewModel(
            workoutSummaryRepository = FakeWorkoutSummaryRepository(historyItems = emptyList(), throwOnHistory = true),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(!state.isLoading)
        assertNotNull(state.errorMessage)
        assertEquals(0, state.items.size)
    }
}

private class FakeWorkoutSummaryRepository(
    private val historyItems: List<WorkoutHistoryItem>,
    private val throwOnHistory: Boolean = false,
) : WorkoutSummaryRepository {
    override suspend fun saveSummary(summary: WorkoutSummary): Long = 1L
    override suspend fun getAllSummaries(): List<WorkoutSummary> = emptyList()
    override suspend fun getSummaryBySessionId(sessionId: Long): WorkoutSummary? = null
    override suspend fun getHistory(): List<WorkoutHistoryItem> {
        if (throwOnHistory) throw IllegalStateException("history load failed")
        return historyItems
    }
}
