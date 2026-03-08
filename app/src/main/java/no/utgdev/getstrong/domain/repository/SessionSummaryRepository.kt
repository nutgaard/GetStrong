package no.utgdev.getstrong.domain.repository

import no.utgdev.getstrong.domain.model.WorkoutSessionSummary

interface SessionSummaryRepository {
    suspend fun getSessionSummary(sessionId: Long): WorkoutSessionSummary?
}
