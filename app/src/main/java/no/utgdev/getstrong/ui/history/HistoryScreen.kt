package no.utgdev.getstrong.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onRetry: () -> Unit,
    onStartWorkoutFlow: () -> Unit,
    onOpenExerciseHistory: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedSection by rememberSaveable { mutableStateOf(HistorySection.LIST) }
    var displayedMonth by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = remember(displayedMonth) { YearMonth.parse(displayedMonth) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("History") },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PrimaryTabRow(selectedTabIndex = selectedSection.ordinal) {
                    HistorySection.entries.forEach { section ->
                        Tab(
                            selected = selectedSection == section,
                            onClick = { selectedSection = section },
                            text = { Text(section.title) },
                        )
                    }
                }
            }

            when {
                uiState.isLoading -> {
                    item { MessageCard("Loading history...", null, null) }
                }
                uiState.errorMessage != null -> {
                    item {
                        MessageCard(
                            title = uiState.errorMessage,
                            actionLabel = "Retry",
                            onAction = onRetry,
                        )
                    }
                }
                uiState.workouts.isEmpty() -> {
                    item {
                        MessageCard(
                            title = "No workout history yet.",
                            body = "Complete a workout to see list and calendar history here.",
                            actionLabel = "Start a Workout",
                            onAction = onStartWorkoutFlow,
                        )
                    }
                }
                selectedSection == HistorySection.LIST -> {
                    item {
                        Text(
                            text = "Completed Workouts",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                    items(uiState.workouts, key = { it.sessionId }) { workout ->
                        HistoryWorkoutCard(
                            workout = workout,
                            onOpenExerciseHistory = onOpenExerciseHistory,
                        )
                    }
                }
                else -> {
                    item {
                        CalendarHeader(
                            month = month,
                            onPrevious = {
                                displayedMonth = month.minusMonths(1).toString()
                            },
                            onNext = {
                                displayedMonth = month.plusMonths(1).toString()
                            },
                        )
                    }
                    item {
                        HistoryCalendarCard(
                            month = month,
                            workouts = uiState.workouts,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseHistoryScreen(
    uiState: ExerciseHistoryUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.exerciseName.isBlank()) "Exercise History" else uiState.exerciseName,
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                uiState.isLoading -> {
                    item { MessageCard("Loading exercise history...", null, null) }
                }
                uiState.errorMessage != null -> {
                    item {
                        MessageCard(
                            title = uiState.errorMessage,
                            actionLabel = "Retry",
                            onAction = onRetry,
                        )
                    }
                }
                uiState.rows.isEmpty() -> {
                    item {
                        MessageCard(
                            title = "No work-set history yet.",
                            body = "This exercise has no completed non-warmup sets in saved session history.",
                        )
                    }
                }
                else -> {
                    item {
                        Text(
                            text = "Work Set History",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                    items(uiState.rows, key = { "${it.sessionId}-${it.completedAtEpochMs}-${it.reps}-${it.weightKg}" }) { row ->
                        ExerciseHistoryRowCard(row = row)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryWorkoutCard(
    workout: HistoryWorkoutCardUi,
    onOpenExerciseHistory: (Long) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(workout.workoutName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        formatDate(workout.completedAtEpochMs),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatElapsedDuration(workout.totalDurationSeconds),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        formatWeight(workout.totalVolumeKg),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (workout.exerciseResults.isEmpty()) {
                Text(
                    text = "No per-exercise work sets recorded for this session.",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                workout.exerciseResults.forEach { result ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onOpenExerciseHistory(result.exerciseId) }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(result.exerciseName, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                result.resultSummary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            text = "View",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseHistoryRowCard(row: ExerciseHistoryRowUi) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(row.workoutName, style = MaterialTheme.typography.titleMedium)
                    Text(formatDate(row.completedAtEpochMs), style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = "Session ${row.sessionId}",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HistoryMetric(label = "Reps", value = row.reps.toString(), modifier = Modifier.weight(1f))
                HistoryMetric(label = "Weight", value = formatWeight(row.weightKg), modifier = Modifier.weight(1f))
                HistoryMetric(label = "Est. 1RM", value = formatWeight(row.estimatedOneRepMaxKg), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HistoryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun CalendarHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onPrevious) { Text("Prev") }
        Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onNext) { Text("Next") }
    }
}

@Composable
private fun HistoryCalendarCard(
    month: YearMonth,
    workouts: List<HistoryWorkoutCardUi>,
) {
    val completionCounts = workouts
        .groupingBy { epochMsToLocalDate(it.completedAtEpochMs) }
        .eachCount()
    val firstDay = month.atDay(1)
    val leadingSlots = firstDay.dayOfWeek.value % 7
    val daysInMonth = month.lengthOfMonth()
    val weekDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                weekDays.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            repeat(((leadingSlots + daysInMonth + 6) / 7)) { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    repeat(7) { column ->
                        val cellIndex = week * 7 + column
                        val dayNumber = cellIndex - leadingSlots + 1
                        if (dayNumber !in 1..daysInMonth) {
                            Box(modifier = Modifier.weight(1f).height(44.dp))
                        } else {
                            val date = month.atDay(dayNumber)
                            val count = completionCounts[date] ?: 0
                            CalendarDayCell(
                                dayNumber = dayNumber,
                                workoutCount = count,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    workoutCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.height(44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (workoutCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = dayNumber.toString(),
                color = if (workoutCount > 0) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (workoutCount > 0) FontWeight.Bold else FontWeight.Normal,
            )
        }
        if (workoutCount > 1) {
            Text(
                text = "$workoutCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun MessageCard(
    title: String,
    body: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (body != null) {
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
            if (actionLabel != null && onAction != null) {
                Button(
                    onClick = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

private enum class HistorySection(val title: String) {
    LIST("List"),
    CALENDAR("Calendar"),
}

private fun epochMsToLocalDate(epochMs: Long): LocalDate =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate()

private fun formatDate(epochMs: Long): String =
    epochMsToLocalDate(epochMs).format(DateTimeFormatter.ofPattern("d MMM yyyy"))

private fun formatWeight(weightKg: Double): String =
    if (weightKg % 1.0 == 0.0) {
        "${weightKg.toInt()} kg"
    } else {
        "${"%.1f".format(weightKg)} kg"
    }

private fun formatElapsedDuration(seconds: Long): String {
    val total = seconds.coerceAtLeast(0L)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}
