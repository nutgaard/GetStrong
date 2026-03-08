package no.utgdev.getstrong.domain.usecase

import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.SessionSetType
import no.utgdev.getstrong.domain.model.SetResult
import no.utgdev.getstrong.domain.model.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionSummaryCalculatorTest {
    private val calculator = SessionSummaryCalculator()

    @Test
    fun computesDurationAndVolumeUsingWorkSetsOnly() {
        val session = WorkoutSession(
            id = 1,
            workoutId = 10,
            startedAtEpochMs = 1_000L,
            endedAtEpochMs = 61_000L,
        )
        val planned = listOf(
            SessionPlannedSet(
                id = 100,
                sessionId = 1,
                workoutSlotId = 20,
                setOrder = 0,
                exerciseId = 1006,
                setType = SessionSetType.WARMUP,
                targetReps = 3,
                targetWeightKg = 60.0,
            ),
            SessionPlannedSet(
                id = 101,
                sessionId = 1,
                workoutSlotId = 20,
                setOrder = 1,
                exerciseId = 1006,
                setType = SessionSetType.WORK,
                targetReps = 5,
                targetWeightKg = 100.0,
            ),
        )
        val results = listOf(
            SetResult(
                id = 1,
                sessionId = 1,
                plannedSetId = 100,
                workoutSlotId = 20,
                exerciseId = 1006,
                setType = SessionSetType.WARMUP,
                reps = 3,
                weightKg = 60.0,
            ),
            SetResult(
                id = 2,
                sessionId = 1,
                plannedSetId = 101,
                workoutSlotId = 20,
                exerciseId = 1006,
                setType = SessionSetType.WORK,
                reps = 5,
                weightKg = 100.0,
            ),
        )

        val summary = calculator.calculate(session, planned, results)

        assertEquals(60L, summary.totalDurationSeconds)
        assertEquals(500.0, summary.totalVolumeKg, 0.0)
        assertEquals(SessionSummaryCalculator.VOLUME_RULE, summary.volumeRule)
        assertEquals(2, summary.sets.size)
        assertEquals(3, summary.sets[0].achievedReps)
        assertEquals(5, summary.sets[1].achievedReps)
    }
}
