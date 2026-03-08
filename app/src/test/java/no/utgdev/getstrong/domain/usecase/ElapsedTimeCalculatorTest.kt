package no.utgdev.getstrong.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class ElapsedTimeCalculatorTest {
    private val calculator = ElapsedTimeCalculator()

    @Test
    fun computesElapsedUsingEndWhenAvailable() {
        assertEquals(60L, calculator.elapsedSeconds(1_000L, 61_000L, 90_000L))
    }

    @Test
    fun computesElapsedUsingNowWhenSessionActive() {
        assertEquals(30L, calculator.elapsedSeconds(1_000L, null, 31_000L))
    }

    @Test
    fun clampsNegativeDurationToZero() {
        assertEquals(0L, calculator.elapsedSeconds(10_000L, 9_000L, 9_000L))
    }
}
