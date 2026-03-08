package no.utgdev.getstrong.domain.repository

import no.utgdev.getstrong.domain.model.WorkoutHistoryItem
import no.utgdev.getstrong.domain.model.WorkoutSummary

interface WorkoutSummaryRepository {
    suspend fun saveSummary(summary: WorkoutSummary): Long
    suspend fun getAllSummaries(): List<WorkoutSummary>
    suspend fun getSummaryBySessionId(sessionId: Long): WorkoutSummary?
    suspend fun getHistory(): List<WorkoutHistoryItem>
}
