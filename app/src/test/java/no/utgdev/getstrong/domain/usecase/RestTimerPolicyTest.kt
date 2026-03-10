package no.utgdev.getstrong.domain.usecase

import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.SessionSetType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestTimerPolicyTest {
    private val policy = RestTimerPolicy()

    @Test
    fun startsAfterLastWarmupBeforeWorkForSameSlot() {
        val completed = set(1, 10, SessionSetType.WARMUP)
        val next = set(2, 10, SessionSetType.WORK)
        assertTrue(policy.shouldStartForTransition(completed, next))
    }

    @Test
    fun doesNotStartBetweenWarmupSets() {
        val completed = set(1, 10, SessionSetType.WARMUP)
        val next = set(2, 10, SessionSetType.WARMUP)
        assertFalse(policy.shouldStartForTransition(completed, next))
    }

    @Test
    fun startsBetweenWorkSetsForSameSlot() {
        assertTrue(policy.shouldStartForTransition(set(1, 10, SessionSetType.WORK), set(2, 10, SessionSetType.WORK)))
    }

    @Test
    fun doesNotStartForDifferentSlotOrWhenNextSetIsWarmup() {
        assertFalse(policy.shouldStartForTransition(set(1, 10, SessionSetType.WARMUP), set(2, 11, SessionSetType.WORK)))
        assertFalse(policy.shouldStartForTransition(set(1, 10, SessionSetType.WORK), set(2, 10, SessionSetType.WARMUP)))
    }

    private fun set(id: Long, slotId: Long, type: String): SessionPlannedSet =
        SessionPlannedSet(
            id = id,
            sessionId = 1,
            workoutSlotId = slotId,
            setOrder = id.toInt(),
            exerciseId = 100,
            setType = type,
            targetReps = 5,
        )
}
