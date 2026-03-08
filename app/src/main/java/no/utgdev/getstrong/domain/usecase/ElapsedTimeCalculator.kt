package no.utgdev.getstrong.domain.usecase

import javax.inject.Inject

class ElapsedTimeCalculator @Inject constructor() {
    fun elapsedSeconds(startedAtEpochMs: Long, endedAtEpochMs: Long?, nowEpochMs: Long): Long {
        val end = endedAtEpochMs ?: nowEpochMs
        return ((end - startedAtEpochMs).coerceAtLeast(0L)) / 1000L
    }
}
