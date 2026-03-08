package no.utgdev.getstrong.data.repository

import javax.inject.Inject
import no.utgdev.getstrong.data.local.dao.WorkoutSummaryDao
import no.utgdev.getstrong.data.local.entity.WorkoutSummaryEntity
import no.utgdev.getstrong.domain.model.WorkoutHistoryItem
import no.utgdev.getstrong.domain.model.WorkoutSummary
import no.utgdev.getstrong.domain.repository.WorkoutSummaryRepository

class WorkoutSummaryRepositoryImpl @Inject constructor(
    private val summaryDao: WorkoutSummaryDao,
) : WorkoutSummaryRepository {
    override suspend fun saveSummary(summary: WorkoutSummary): Long {
        val existing = summaryDao.getSummaryBySessionId(summary.sessionId)
        return summaryDao.upsertSummary(summary.toEntity(existingId = existing?.id ?: 0L))
    }

    override suspend fun getAllSummaries(): List<WorkoutSummary> =
        summaryDao.getAllSummaries().map { it.toDomain() }

    override suspend fun getSummaryBySessionId(sessionId: Long): WorkoutSummary? =
        summaryDao.getSummaryBySessionId(sessionId)?.toDomain()

    override suspend fun getHistory(): List<WorkoutHistoryItem> =
        summaryDao.getAllSummaries().map {
            WorkoutHistoryItem(
                id = it.id,
                sessionId = it.sessionId,
                workoutName = it.workoutName,
                totalVolumeKg = it.totalVolumeKg,
                totalDurationSeconds = it.totalDurationSeconds,
                completedAtEpochMs = it.completedAtEpochMs,
            )
        }
}

private fun WorkoutSummary.toEntity(existingId: Long): WorkoutSummaryEntity =
    WorkoutSummaryEntity(
        id = existingId,
        workoutId = workoutId,
        sessionId = sessionId,
        workoutName = workoutName,
        totalVolumeKg = totalVolumeKg,
        totalDurationSeconds = totalDurationSeconds,
        completedAtEpochMs = completedAtEpochMs,
    )

private fun WorkoutSummaryEntity.toDomain(): WorkoutSummary =
    WorkoutSummary(
        id = id,
        workoutId = workoutId,
        sessionId = sessionId,
        workoutName = workoutName,
        totalVolumeKg = totalVolumeKg,
        totalDurationSeconds = totalDurationSeconds,
        completedAtEpochMs = completedAtEpochMs,
    )
