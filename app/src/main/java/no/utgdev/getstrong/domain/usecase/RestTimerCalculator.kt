package no.utgdev.getstrong.domain.usecase

import javax.inject.Inject
import kotlin.math.max

class RestTimerCalculator @Inject constructor() {
    fun remainingSeconds(nowMs: Long, endAtMs: Long): Int {
        val remainingMs = max(0L, endAtMs - nowMs)
        return ((remainingMs + 999L) / 1000L).toInt()
    }
}
