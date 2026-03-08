package no.utgdev.getstrong.data.repository

import javax.inject.Inject
import no.utgdev.getstrong.data.local.dao.WorkoutSummaryDao
import no.utgdev.getstrong.data.local.entity.WorkoutSummaryEntity
import no.utgdev.getstrong.domain.model.WorkoutSummary
import no.utgdev.getstrong.domain.repository.WorkoutSummaryRepository

class WorkoutSummaryRepositoryImpl @Inject constructor(
    private val summaryDao: WorkoutSummaryDao,
) : WorkoutSummaryRepository {
    override suspend fun saveSummary(summary: WorkoutSummary): Long =
        summaryDao.upsertSummary(summary.toEntity())

    override suspend fun getAllSummaries(): List<WorkoutSummary> =
        summaryDao.getAllSummaries().map { it.toDomain() }
}

private fun WorkoutSummary.toEntity(): WorkoutSummaryEntity =
    WorkoutSummaryEntity(
        id = id,
        workoutId = workoutId,
        sessionId = sessionId,
        totalVolumeKg = totalVolumeKg,
        totalDurationSeconds = totalDurationSeconds,
        completedAtEpochMs = completedAtEpochMs,
    )

private fun WorkoutSummaryEntity.toDomain(): WorkoutSummary =
    WorkoutSummary(
        id = id,
        workoutId = workoutId,
        sessionId = sessionId,
        totalVolumeKg = totalVolumeKg,
        totalDurationSeconds = totalDurationSeconds,
        completedAtEpochMs = completedAtEpochMs,
    )
