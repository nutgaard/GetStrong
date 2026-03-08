package no.utgdev.getstrong.data.repository

import javax.inject.Inject
import no.utgdev.getstrong.data.local.dao.SessionDao
import no.utgdev.getstrong.data.local.entity.SetResultEntity
import no.utgdev.getstrong.data.local.entity.WorkoutSessionEntity
import no.utgdev.getstrong.domain.model.SetResult
import no.utgdev.getstrong.domain.model.WorkoutSession
import no.utgdev.getstrong.domain.repository.SessionRepository

class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
) : SessionRepository {
    override suspend fun saveSession(session: WorkoutSession): Long =
        sessionDao.upsertSession(session.toEntity())

    override suspend fun saveSetResult(result: SetResult): Long =
        sessionDao.upsertSetResult(result.toEntity())

    override suspend fun getSetResults(sessionId: Long): List<SetResult> =
        sessionDao.getSetResults(sessionId).map { it.toDomain() }
}

private fun WorkoutSession.toEntity(): WorkoutSessionEntity =
    WorkoutSessionEntity(
        id = id,
        workoutId = workoutId,
        startedAtEpochMs = startedAtEpochMs,
        endedAtEpochMs = endedAtEpochMs,
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
