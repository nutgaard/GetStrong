package no.utgdev.getstrong.data.repository

import javax.inject.Inject
import kotlin.math.round
import no.utgdev.getstrong.data.local.dao.ExerciseHistoryRow
import no.utgdev.getstrong.data.local.dao.SessionDao
import no.utgdev.getstrong.data.local.entity.SessionPlannedSetEntity
import no.utgdev.getstrong.data.local.entity.SetResultEntity
import no.utgdev.getstrong.data.local.entity.WorkoutSessionEntity
import no.utgdev.getstrong.domain.model.ExerciseHistoryEntry
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.SetResult
import no.utgdev.getstrong.domain.model.WorkoutSession
import no.utgdev.getstrong.domain.model.WorkoutSessionSummary
import no.utgdev.getstrong.domain.repository.SessionSummaryRepository
import no.utgdev.getstrong.domain.usecase.SessionSummaryCalculator

class SessionSummaryRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val sessionSummaryCalculator: SessionSummaryCalculator,
) : SessionSummaryRepository {
    override suspend fun getSessionSummary(sessionId: Long): WorkoutSessionSummary? {
        val session = sessionDao.getSession(sessionId)?.toDomain() ?: return null
        val plannedSets = sessionDao.getPlannedSets(sessionId).map { it.toDomain() }
        val setResults = sessionDao.getSetResults(sessionId).map { it.toDomain() }
        return sessionSummaryCalculator.calculate(
            session = session,
            plannedSets = plannedSets,
            setResults = setResults,
        )
    }

    override suspend fun getExerciseHistory(exerciseId: Long): List<ExerciseHistoryEntry> =
        sessionDao.getExerciseHistoryRows(exerciseId).map { row -> row.toDomain() }
}

private fun WorkoutSessionEntity.toDomain(): WorkoutSession =
    WorkoutSession(
        id = id,
        workoutId = workoutId,
        startedAtEpochMs = startedAtEpochMs,
        endedAtEpochMs = endedAtEpochMs,
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

private fun ExerciseHistoryRow.toDomain(): ExerciseHistoryEntry =
    ExerciseHistoryEntry(
        exerciseId = exerciseId,
        sessionId = sessionId,
        workoutName = workoutName.ifBlank { "Workout $sessionId" },
        completedAtEpochMs = completedAtEpochMs,
        reps = reps,
        weightKg = weightKg,
        estimatedOneRepMaxKg = round(weightKg * (1.0 + reps / 30.0) * 10.0) / 10.0,
    )
