package no.utgdev.getstrong.data.repository

import javax.inject.Inject
import no.utgdev.getstrong.data.local.dao.SessionDao
import no.utgdev.getstrong.data.local.dao.SlotProgressionRecord
import no.utgdev.getstrong.data.local.entity.SessionPlannedSetEntity
import no.utgdev.getstrong.data.local.entity.SetResultEntity
import no.utgdev.getstrong.data.local.entity.WorkoutSessionEntity
import no.utgdev.getstrong.domain.model.ActiveSessionState
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.SetResult
import no.utgdev.getstrong.domain.model.SlotProgressionUpdate
import no.utgdev.getstrong.domain.model.WorkoutSession
import no.utgdev.getstrong.domain.repository.SessionRepository

class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
) : SessionRepository {
    override suspend fun saveSession(session: WorkoutSession): Long =
        sessionDao.upsertSession(session.toEntity())

    override suspend fun startSession(workoutId: Long, plannedSets: List<SessionPlannedSet>): Long {
        val sessionId = sessionDao.createSessionWithPlan(
            session = WorkoutSessionEntity(
                workoutId = workoutId,
                startedAtEpochMs = System.currentTimeMillis(),
                endedAtEpochMs = null,
            ),
            plannedSets = plannedSets.map { it.toEntity(sessionId = 0) },
        )
        return sessionId
    }

    override suspend fun findUnfinishedSessionId(): Long? =
        sessionDao.getLatestUnfinishedSessionId()

    override suspend fun discardSessionIfNoProgress(sessionId: Long): Boolean {
        val session = sessionDao.getSession(sessionId) ?: return false
        if (session.endedAtEpochMs != null) return false
        val hasProgress = sessionDao.getPlannedSets(sessionId).any { plannedSet ->
            plannedSet.isCompleted || (plannedSet.completedReps ?: 0) > 0
        }
        if (hasProgress) return false
        sessionDao.discardSession(sessionId)
        return true
    }

    override suspend fun getActiveSessionState(sessionId: Long): ActiveSessionState? {
        val session = sessionDao.getSession(sessionId)?.toDomain() ?: return null
        val sets = sessionDao.getPlannedSets(sessionId).map { it.toDomain() }
        return ActiveSessionState(
            session = session,
            plannedSets = sets,
        )
    }

    override suspend fun completePlannedSet(
        sessionId: Long,
        plannedSetId: Long,
        repsAchieved: Int,
    ): ActiveSessionState? {
        val plannedSet = sessionDao.getPlannedSet(sessionId, plannedSetId) ?: return null
        val normalizedReps = repsAchieved.coerceAtLeast(0)
        val isCompleted = normalizedReps > 0
        sessionDao.updatePlannedSetCompletion(
            sessionId = sessionId,
            plannedSetId = plannedSetId,
            isCompleted = isCompleted,
            completedReps = normalizedReps.takeIf { isCompleted },
        )
        if (isCompleted) {
            val existingResult = sessionDao.getSetResultForPlannedSet(sessionId, plannedSetId)
            sessionDao.upsertSetResult(
                SetResultEntity(
                    id = existingResult?.id ?: 0L,
                    sessionId = sessionId,
                    plannedSetId = plannedSetId,
                    workoutSlotId = plannedSet.workoutSlotId,
                    exerciseId = plannedSet.exerciseId,
                    setType = plannedSet.setType,
                    reps = normalizedReps,
                    weightKg = existingResult?.weightKg ?: plannedSet.targetWeightKg ?: 0.0,
                ),
            )
        } else {
            sessionDao.deleteSetResultForPlannedSet(sessionId, plannedSetId)
        }
        return getActiveSessionState(sessionId)
    }

    override suspend fun updatePlannedSetWeight(
        sessionId: Long,
        plannedSetId: Long,
        weightKg: Double,
    ): ActiveSessionState? {
        val normalizedWeight = weightKg.coerceAtLeast(0.0)
        val plannedSet = sessionDao.getPlannedSet(sessionId, plannedSetId) ?: return null
        sessionDao.updatePlannedSetWeight(sessionId, plannedSetId, normalizedWeight)
        val existingResult = sessionDao.getSetResultForPlannedSet(sessionId, plannedSetId)
        if (existingResult != null) {
            sessionDao.upsertSetResult(existingResult.copy(weightKg = normalizedWeight))
        } else if (plannedSet.isCompleted && (plannedSet.completedReps ?: 0) > 0) {
            sessionDao.upsertSetResult(
                SetResultEntity(
                    sessionId = sessionId,
                    plannedSetId = plannedSetId,
                    workoutSlotId = plannedSet.workoutSlotId,
                    exerciseId = plannedSet.exerciseId,
                    setType = plannedSet.setType,
                    reps = plannedSet.completedReps ?: 0,
                    weightKg = normalizedWeight,
                ),
            )
        }
        return getActiveSessionState(sessionId)
    }

    override suspend fun addExtraSet(sessionId: Long, anchorPlannedSetId: Long): ActiveSessionState? {
        val plannedSets = sessionDao.getPlannedSets(sessionId)
        val anchor = plannedSets.firstOrNull { it.id == anchorPlannedSetId } ?: return null
        val reordered = buildList {
            plannedSets.forEach { plannedSet ->
                add(plannedSet)
                if (plannedSet.id == anchor.id) {
                    add(
                        plannedSet.copy(
                            id = 0L,
                            setOrder = plannedSet.setOrder + 1,
                            isCompleted = false,
                            completedReps = null,
                            isExtra = true,
                        ),
                    )
                }
            }
        }.mapIndexed { index, plannedSet -> plannedSet.copy(setOrder = index) }
        sessionDao.replaceSessionPlan(sessionId, reordered)
        return getActiveSessionState(sessionId)
    }

    override suspend fun removeExtraSet(sessionId: Long, plannedSetId: Long): ActiveSessionState? {
        val plannedSets = sessionDao.getPlannedSets(sessionId)
        val target = plannedSets.firstOrNull { it.id == plannedSetId } ?: return null
        if (!target.isExtra) return getActiveSessionState(sessionId)
        val reordered = plannedSets
            .filterNot { it.id == plannedSetId }
            .mapIndexed { index, plannedSet -> plannedSet.copy(setOrder = index) }
        sessionDao.deleteSetResultForPlannedSet(sessionId, plannedSetId)
        sessionDao.replaceSessionPlan(sessionId, reordered)
        return getActiveSessionState(sessionId)
    }

    override suspend fun completeSession(sessionId: Long) {
        sessionDao.markSessionCompleted(sessionId, System.currentTimeMillis())
    }

    override suspend fun completeSessionWithProgression(
        sessionId: Long,
        updates: List<SlotProgressionUpdate>,
    ) {
        sessionDao.completeSessionWithProgression(
            sessionId = sessionId,
            endedAtEpochMs = System.currentTimeMillis(),
            updates = updates.map {
                SlotProgressionRecord(
                    slotId = it.slotId,
                    nextTargetReps = it.nextTargetReps,
                    nextWorkingWeightKg = it.nextWorkingWeightKg,
                    nextFailureStreak = it.nextFailureStreak,
                )
            },
        )
    }

    override suspend fun completeSessionWithProgressionAndPersistSummary(
        sessionId: Long,
        updates: List<SlotProgressionUpdate>,
    ): Boolean {
        sessionDao.completeSessionWithProgressionAndPersistSummary(
            sessionId = sessionId,
            endedAtEpochMs = System.currentTimeMillis(),
            updates = updates.map {
                SlotProgressionRecord(
                    slotId = it.slotId,
                    nextTargetReps = it.nextTargetReps,
                    nextWorkingWeightKg = it.nextWorkingWeightKg,
                    nextFailureStreak = it.nextFailureStreak,
                )
            },
        )
        return true
    }

    override suspend fun saveSetResult(result: SetResult): Long =
        sessionDao.upsertSetResult(result.toEntity())

    override suspend fun getSetResults(sessionId: Long): List<SetResult> =
        sessionDao.getSetResults(sessionId).map { it.toDomain() }
}

private fun WorkoutSessionEntity.toDomain(): WorkoutSession =
    WorkoutSession(
        id = id,
        workoutId = workoutId,
        startedAtEpochMs = startedAtEpochMs,
        endedAtEpochMs = endedAtEpochMs,
    )

private fun WorkoutSession.toEntity(): WorkoutSessionEntity =
    WorkoutSessionEntity(
        id = id,
        workoutId = workoutId,
        startedAtEpochMs = startedAtEpochMs,
        endedAtEpochMs = endedAtEpochMs,
    )

private fun SessionPlannedSet.toEntity(sessionId: Long): SessionPlannedSetEntity =
    SessionPlannedSetEntity(
        id = id,
        sessionId = sessionId,
        workoutSlotId = workoutSlotId,
        setOrder = setOrder,
        exerciseId = exerciseId,
        setType = setType,
        targetReps = targetReps,
        targetWeightKg = targetWeightKg,
        isCompleted = isCompleted,
        completedReps = completedReps,
        isExtra = isExtra,
    )

private fun SessionPlannedSetEntity.toDomain(): SessionPlannedSet =
    SessionPlannedSet(
        id = id,
        sessionId = sessionId,
        workoutSlotId = workoutSlotId,
        setOrder = setOrder,
        exerciseId = exerciseId,
        setType = setType,
        targetReps = targetReps,
        targetWeightKg = targetWeightKg,
        isCompleted = isCompleted,
        completedReps = completedReps,
        isExtra = isExtra,
    )

private fun SetResult.toEntity(): SetResultEntity =
    SetResultEntity(
        id = id,
        sessionId = sessionId,
        plannedSetId = plannedSetId,
        workoutSlotId = workoutSlotId,
        exerciseId = exerciseId,
        setType = setType,
        reps = reps,
        weightKg = weightKg,
    )

private fun SetResultEntity.toDomain(): SetResult =
    SetResult(
        id = id,
        sessionId = sessionId,
        plannedSetId = plannedSetId,
        workoutSlotId = workoutSlotId,
        exerciseId = exerciseId,
        setType = setType,
        reps = reps,
        weightKg = weightKg,
    )
