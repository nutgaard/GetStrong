package no.utgdev.getstrong.ui.history

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryCalendarContractTest {
    @Test
    fun mondayFirstLeadingSlotsUsesMondayAsColumnZero() {
        assertEquals(0, mondayFirstLeadingSlots(LocalDate.parse("2026-03-02"))) // Monday
        assertEquals(6, mondayFirstLeadingSlots(LocalDate.parse("2026-03-01"))) // Sunday
    }

    @Test
    fun workoutsGroupedByDateKeepsSingleDateBucketForMultipleWorkouts() {
        val workouts = listOf(
            historyWorkout(sessionId = 1L, completedAtEpochMs = epochMsAtNoon("2026-03-10")),
            historyWorkout(sessionId = 2L, completedAtEpochMs = epochMsAtNoon("2026-03-10")),
            historyWorkout(sessionId = 3L, completedAtEpochMs = epochMsAtNoon("2026-03-11")),
        )

        val grouped = workoutsGroupedByDate(workouts)

        assertEquals(2, grouped.size)
        assertEquals(2, grouped[LocalDate.parse("2026-03-10")]?.size)
        assertEquals(1, grouped[LocalDate.parse("2026-03-11")]?.size)
    }

    @Test
    fun calendarDaySelectionReturnsNoneSingleOrMultiple() {
        assertEquals(CalendarDaySelection.NONE, calendarDaySelection(emptyList()))
        assertEquals(CalendarDaySelection.SINGLE, calendarDaySelection(listOf(historyWorkout(sessionId = 1L, completedAtEpochMs = epochMsAtNoon("2026-03-10")))))
        assertEquals(
            CalendarDaySelection.MULTIPLE,
            calendarDaySelection(
                listOf(
                    historyWorkout(sessionId = 1L, completedAtEpochMs = epochMsAtNoon("2026-03-10")),
                    historyWorkout(sessionId = 2L, completedAtEpochMs = epochMsAtNoon("2026-03-10")),
                ),
            ),
        )
    }
}

private fun historyWorkout(
    sessionId: Long,
    completedAtEpochMs: Long,
): HistoryWorkoutCardUi =
    HistoryWorkoutCardUi(
        id = sessionId,
        sessionId = sessionId,
        workoutName = "Workout $sessionId",
        totalVolumeKg = 1000.0,
        totalDurationSeconds = 1800L,
        completedAtEpochMs = completedAtEpochMs,
        exerciseResults = emptyList(),
    )

private fun epochMsAtNoon(dateIso: String): Long =
    LocalDate.parse(dateIso)
        .atTime(12, 0)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
