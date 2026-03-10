package no.utgdev.getstrong.domain.repository

import no.utgdev.getstrong.domain.model.SetResult
import no.utgdev.getstrong.domain.model.ActiveSessionState
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.SlotProgressionUpdate
import no.utgdev.getstrong.domain.model.WorkoutSession

interface SessionRepository {
    suspend fun startSession(workoutId: Long, plannedSets: List<SessionPlannedSet>): Long
    suspend fun findUnfinishedSessionId(): Long?
    suspend fun discardSessionIfNoProgress(sessionId: Long): Boolean
    suspend fun getActiveSessionState(sessionId: Long): ActiveSessionState?
    suspend fun completePlannedSet(sessionId: Long, plannedSetId: Long, repsAchieved: Int): ActiveSessionState?
    suspend fun updatePlannedSetWeight(sessionId: Long, plannedSetId: Long, weightKg: Double): ActiveSessionState?
    suspend fun addExtraSet(sessionId: Long, anchorPlannedSetId: Long): ActiveSessionState?
    suspend fun removeExtraSet(sessionId: Long, plannedSetId: Long): ActiveSessionState?
    suspend fun completeSession(sessionId: Long)
    suspend fun completeSessionWithProgression(sessionId: Long, updates: List<SlotProgressionUpdate>)
    suspend fun saveSession(session: WorkoutSession): Long
    suspend fun saveSetResult(result: SetResult): Long
    suspend fun getSetResults(sessionId: Long): List<SetResult>
}
