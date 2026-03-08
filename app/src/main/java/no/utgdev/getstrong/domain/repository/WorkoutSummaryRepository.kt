package no.utgdev.getstrong.domain.repository

import no.utgdev.getstrong.domain.model.WorkoutSummary

interface WorkoutSummaryRepository {
    suspend fun saveSummary(summary: WorkoutSummary): Long
    suspend fun getAllSummaries(): List<WorkoutSummary>
}
