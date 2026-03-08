package no.utgdev.getstrong.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class RestTimerCalculatorTest {
    private val calculator = RestTimerCalculator()

    @Test
    fun roundsUpRemainingSecondsAndStopsAtZero() {
        assertEquals(180, calculator.remainingSeconds(nowMs = 0L, endAtMs = 180_000L))
        assertEquals(1, calculator.remainingSeconds(nowMs = 179_001L, endAtMs = 180_000L))
        assertEquals(0, calculator.remainingSeconds(nowMs = 180_000L, endAtMs = 180_000L))
        assertEquals(0, calculator.remainingSeconds(nowMs = 200_000L, endAtMs = 180_000L))
    }
}
