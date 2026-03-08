package no.utgdev.getstrong.domain.session

import no.utgdev.getstrong.domain.model.ActiveSessionState
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.SessionSetType
import no.utgdev.getstrong.domain.model.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveSessionStateTest {
    @Test
    fun currentSetAdvancesWhenSetsComplete() {
        val state = ActiveSessionState(
            session = WorkoutSession(id = 1, workoutId = 1, startedAtEpochMs = 0),
            plannedSets = listOf(
                SessionPlannedSet(id = 1, sessionId = 1, setOrder = 0, exerciseId = 1006, setType = SessionSetType.WARMUP, targetReps = 3, isCompleted = true),
                SessionPlannedSet(id = 2, sessionId = 1, setOrder = 1, exerciseId = 1006, setType = SessionSetType.WORK, targetReps = 5, isCompleted = false),
            ),
        )

        assertEquals(2L, state.currentSet?.id)
        assertFalse(state.isCompleted)

        val completed = state.copy(
            plannedSets = state.plannedSets.map { it.copy(isCompleted = true) },
        )
        assertTrue(completed.isCompleted)
        assertEquals(null, completed.currentSet)
    }

    @Test
    fun currentSetRemainsEarliestIncompleteWhenLaterSetCompletesOutOfOrder() {
        val state = ActiveSessionState(
            session = WorkoutSession(id = 2, workoutId = 10, startedAtEpochMs = 0),
            plannedSets = listOf(
                SessionPlannedSet(id = 10, sessionId = 2, setOrder = 0, exerciseId = 1001, setType = SessionSetType.WARMUP, targetReps = 3, isCompleted = false),
                SessionPlannedSet(id = 11, sessionId = 2, setOrder = 1, exerciseId = 1001, setType = SessionSetType.WORK, targetReps = 5, isCompleted = true, completedReps = 5),
                SessionPlannedSet(id = 12, sessionId = 2, setOrder = 2, exerciseId = 1002, setType = SessionSetType.WORK, targetReps = 5, isCompleted = false),
            ),
        )

        assertEquals(10L, state.currentSet?.id)
        assertFalse(state.isCompleted)
    }
}
