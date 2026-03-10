package no.utgdev.getstrong.domain.repository

import no.utgdev.getstrong.domain.model.ExerciseHistoryEntry
import no.utgdev.getstrong.domain.model.WorkoutSessionSummary

interface SessionSummaryRepository {
    suspend fun getSessionSummary(sessionId: Long): WorkoutSessionSummary?
    suspend fun getExerciseHistory(exerciseId: Long): List<ExerciseHistoryEntry>
    suspend fun getAllExerciseHistory(): List<ExerciseHistoryEntry>
}
