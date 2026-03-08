package no.utgdev.getstrong.domain.usecase

import javax.inject.Inject
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.SessionSetType
import no.utgdev.getstrong.domain.model.SetResult
import no.utgdev.getstrong.domain.model.WorkoutSession
import no.utgdev.getstrong.domain.model.WorkoutSessionSummary
import no.utgdev.getstrong.domain.model.WorkoutSessionSummarySet

class SessionSummaryCalculator @Inject constructor() {
    companion object {
        const val VOLUME_RULE = "WORK_SETS_ONLY"
    }

    fun calculate(
        session: WorkoutSession,
        plannedSets: List<SessionPlannedSet>,
        setResults: List<SetResult>,
    ): WorkoutSessionSummary {
        val resultsByPlannedSetId = setResults
            .mapNotNull { result -> result.plannedSetId?.let { it to result } }
            .toMap()

        val rows = plannedSets.sortedBy { it.setOrder }.map { planned ->
            val result = resultsByPlannedSetId[planned.id]
            WorkoutSessionSummarySet(
                setOrder = planned.setOrder,
                setType = planned.setType,
                exerciseId = planned.exerciseId,
                targetReps = planned.targetReps,
                achievedReps = result?.reps ?: planned.completedReps,
                loadKg = result?.weightKg ?: planned.targetWeightKg,
            )
        }

        val totalVolume = rows
            .asSequence()
            .filter { it.setType == SessionSetType.WORK }
            .sumOf { row -> (row.achievedReps ?: 0) * (row.loadKg ?: 0.0) }

        val endedAt = session.endedAtEpochMs ?: session.startedAtEpochMs
        val totalDurationSeconds = ((endedAt - session.startedAtEpochMs).coerceAtLeast(0L)) / 1000L

        return WorkoutSessionSummary(
            sessionId = session.id,
            workoutId = session.workoutId,
            totalVolumeKg = totalVolume,
            totalDurationSeconds = totalDurationSeconds,
            volumeRule = VOLUME_RULE,
            sets = rows,
        )
    }
}
