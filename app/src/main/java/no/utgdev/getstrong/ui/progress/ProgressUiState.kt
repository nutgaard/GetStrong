package no.utgdev.getstrong.ui.progress

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import no.utgdev.getstrong.domain.model.ExerciseHistoryEntry

data class ProgressUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val exercises: List<ProgressExerciseOverviewUi> = emptyList(),
)

data class ProgressExerciseOverviewUi(
    val exerciseId: Long,
    val exerciseName: String,
    val latestWeightKg: Double,
    val latestReps: Int,
    val latestEstimatedOneRepMaxKg: Double,
    val latestCompletedAtEpochMs: Long,
    val trendPoints: List<Double>,
)

data class ExerciseProgressUiState(
    val exerciseId: Long = 0L,
    val exerciseName: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val selectedRange: ProgressRangeOption = ProgressRangeOption.THREE_MONTHS,
    val points: List<ExerciseProgressPointUi> = emptyList(),
    val latestWeightKg: Double = 0.0,
    val latestReps: Int = 0,
    val bestEstimatedOneRepMaxKg: Double = 0.0,
    val totalSessions: Int = 0,
)

data class ExerciseProgressPointUi(
    val sessionId: Long,
    val completedAtEpochMs: Long,
    val weightKg: Double,
    val reps: Int,
    val estimatedOneRepMaxKg: Double,
)

enum class ProgressRangeOption(val label: String, val monthsBack: Long?) {
    ONE_MONTH("1M", 1),
    THREE_MONTHS("3M", 3),
    SIX_MONTHS("6M", 6),
    ALL("All", null),
}

internal fun buildSessionBestProgressPoints(entries: List<ExerciseHistoryEntry>): List<ExerciseProgressPointUi> =
    entries
        .groupBy { it.sessionId }
        .values
        .map { sessionEntries ->
            sessionEntries.maxWith(progressEntryComparator)
        }
        .sortedBy { it.completedAtEpochMs }
        .map { entry ->
            ExerciseProgressPointUi(
                sessionId = entry.sessionId,
                completedAtEpochMs = entry.completedAtEpochMs,
                weightKg = entry.weightKg,
                reps = entry.reps,
                estimatedOneRepMaxKg = entry.estimatedOneRepMaxKg,
            )
        }

internal fun latestProgressEntry(entries: List<ExerciseHistoryEntry>): ExerciseHistoryEntry? =
    entries.maxWithOrNull(progressEntryComparator)

internal fun filterProgressPoints(
    points: List<ExerciseProgressPointUi>,
    range: ProgressRangeOption,
): List<ExerciseProgressPointUi> {
    if (range.monthsBack == null || points.isEmpty()) return points
    val latestDate = epochMsToLocalDate(points.maxOf { it.completedAtEpochMs })
    val cutoff = latestDate.minusMonths(range.monthsBack)
    return points.filter { point ->
        !epochMsToLocalDate(point.completedAtEpochMs).isBefore(cutoff)
    }
}

private val progressEntryComparator =
    compareBy<ExerciseHistoryEntry>(
        { it.completedAtEpochMs },
        { it.estimatedOneRepMaxKg },
        { it.weightKg },
        { it.reps },
    )

private fun epochMsToLocalDate(epochMs: Long): LocalDate =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate()
