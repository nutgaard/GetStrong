package no.utgdev.getstrong.data.repository

import javax.inject.Inject
import no.utgdev.getstrong.data.local.dao.SessionDao
import no.utgdev.getstrong.data.local.entity.SessionPlannedSetEntity
import no.utgdev.getstrong.data.local.entity.SetResultEntity
import no.utgdev.getstrong.data.local.entity.WorkoutSessionEntity
import no.utgdev.getstrong.domain.model.ActiveSessionState
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.SetResult
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
        val plannedSet = sessionDao.getPlannedSets(sessionId).firstOrNull { it.id == plannedSetId } ?: return null
        sessionDao.markPlannedSetCompleted(
            sessionId = sessionId,
            plannedSetId = plannedSetId,
            completedReps = repsAchieved,
        )
        sessionDao.upsertSetResult(
            SetResultEntity(
                sessionId = sessionId,
                exerciseId = plannedSet.exerciseId,
                setType = plannedSet.setType,
                reps = repsAchieved,
                weightKg = 0.0,
            ),
        )
        return getActiveSessionState(sessionId)
    }

    override suspend fun completeSession(sessionId: Long) {
        sessionDao.markSessionCompleted(sessionId, System.currentTimeMillis())
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
        setOrder = setOrder,
        exerciseId = exerciseId,
        setType = setType,
        targetReps = targetReps,
        isCompleted = isCompleted,
        completedReps = completedReps,
    )

private fun SessionPlannedSetEntity.toDomain(): SessionPlannedSet =
    SessionPlannedSet(
        id = id,
        sessionId = sessionId,
        setOrder = setOrder,
        exerciseId = exerciseId,
        setType = setType,
        targetReps = targetReps,
        isCompleted = isCompleted,
        completedReps = completedReps,
    )

private fun SetResult.toEntity(): SetResultEntity =
    SetResultEntity(
        id = id,
        sessionId = sessionId,
        exerciseId = exerciseId,
        setType = setType,
        reps = reps,
        weightKg = weightKg,
    )

private fun SetResultEntity.toDomain(): SetResult =
    SetResult(
        id = id,
        sessionId = sessionId,
        exerciseId = exerciseId,
        setType = setType,
        reps = reps,
        weightKg = weightKg,
    )
