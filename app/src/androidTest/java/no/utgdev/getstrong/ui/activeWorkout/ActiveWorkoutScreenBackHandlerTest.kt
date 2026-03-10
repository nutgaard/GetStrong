package no.utgdev.getstrong.ui.activeWorkout

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import java.util.concurrent.atomic.AtomicInteger
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.SessionSetType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ActiveWorkoutScreenBackHandlerTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun systemBackInvokesOnExitCallback() {
        val exitCalls = AtomicInteger(0)
        composeRule.setContent {
            ActiveWorkoutScreen(
                uiState = ActiveWorkoutUiState(
                    sessionId = 1L,
                    isLoaded = true,
                    isSessionActive = true,
                    plannedSets = listOf(
                        SessionPlannedSet(
                            id = 1L,
                            sessionId = 1L,
                            workoutSlotId = 11L,
                            setOrder = 0,
                            exerciseId = 1006L,
                            setType = SessionSetType.WORK,
                            targetReps = 5,
                            targetWeightKg = 100.0,
                            isCompleted = false,
                            completedReps = null,
                            isExtra = false,
                        ),
                    ),
                    exerciseNamesById = mapOf(1006L to "Deadlift"),
                    currentSet = SessionPlannedSet(
                        id = 1L,
                        sessionId = 1L,
                        workoutSlotId = 11L,
                        setOrder = 0,
                        exerciseId = 1006L,
                        setType = SessionSetType.WORK,
                        targetReps = 5,
                        targetWeightKg = 100.0,
                        isCompleted = false,
                        completedReps = null,
                        isExtra = false,
                    ),
                ),
                onToggleSet = {},
                onSetReps = { _, _ -> },
                onSetWeight = { _, _ -> },
                onClearSet = {},
                onAddExtraSet = {},
                onRemoveExtraSet = {},
                onFinishSession = {},
                onExit = { exitCalls.incrementAndGet() },
            )
        }

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()

        assertEquals(1, exitCalls.get())
    }
}
