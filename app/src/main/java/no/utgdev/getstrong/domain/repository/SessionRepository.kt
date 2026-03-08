package no.utgdev.getstrong.domain.repository

import no.utgdev.getstrong.domain.model.SetResult
import no.utgdev.getstrong.domain.model.WorkoutSession

interface SessionRepository {
    suspend fun saveSession(session: WorkoutSession): Long
    suspend fun saveSetResult(result: SetResult): Long
    suspend fun getSetResults(sessionId: Long): List<SetResult>
}
