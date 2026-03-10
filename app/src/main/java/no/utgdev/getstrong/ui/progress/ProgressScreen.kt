package no.utgdev.getstrong.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    uiState: ProgressUiState,
    onRetry: () -> Unit,
    onOpenExerciseProgress: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Progress") },
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
                    item { ProgressMessageCard("Loading progress...", null, null) }
                }
                uiState.errorMessage != null -> {
                    item {
                        ProgressMessageCard(
                            title = uiState.errorMessage,
                            actionLabel = "Retry",
                            onAction = onRetry,
                        )
                    }
                }
                uiState.exercises.isEmpty() -> {
                    item {
                        ProgressMessageCard(
                            title = "No tracked exercises yet.",
                            body = "Complete a few work sets to unlock exercise progress charts.",
                        )
                    }
                }
                else -> {
                    item {
                        Text(
                            text = "Tracked Exercises",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                    items(uiState.exercises, key = { it.exerciseId }) { exercise ->
                        ProgressExerciseCard(
                            exercise = exercise,
                            onOpen = { onOpenExerciseProgress(exercise.exerciseId) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseProgressScreen(
    uiState: ExerciseProgressUiState,
    onBack: () -> Unit,
    onRangeSelected: (ProgressRangeOption) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.exerciseName.isBlank()) "Exercise Progress" else uiState.exerciseName)
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
                    item { ProgressMessageCard("Loading progress chart...", null, null) }
                }
                uiState.errorMessage != null -> {
                    item {
                        ProgressMessageCard(
                            title = uiState.errorMessage,
                            actionLabel = "Retry",
                            onAction = onRetry,
                        )
                    }
                }
                else -> {
                    item {
                        RangeSelector(
                            selectedRange = uiState.selectedRange,
                            onRangeSelected = onRangeSelected,
                        )
                    }
                    item {
                        ExerciseProgressChartCard(points = uiState.points)
                    }
                    item {
                        ProgressSummaryMetrics(
                            latestWeightKg = uiState.latestWeightKg,
                            latestReps = uiState.latestReps,
                            bestEstimatedOneRepMaxKg = uiState.bestEstimatedOneRepMaxKg,
                            totalSessions = uiState.totalSessions,
                        )
                    }
                    if (uiState.points.isEmpty()) {
                        item {
                            ProgressMessageCard(
                                title = "No progress points in this range.",
                                body = "Try a wider range or complete more work sets for this exercise.",
                            )
                        }
                    } else {
                        item {
                            Text(
                                text = "Recent Sessions",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.semantics { heading() },
                            )
                        }
                        items(uiState.points.asReversed(), key = { "${it.sessionId}-${it.completedAtEpochMs}" }) { point ->
                            ExerciseProgressPointCard(point = point)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressExerciseCard(
    exercise: ProgressExerciseOverviewUi,
    onOpen: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Latest ${formatWeight(exercise.latestWeightKg)} x ${exercise.latestReps}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Est. 1RM ${formatWeight(exercise.latestEstimatedOneRepMaxKg)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatDate(exercise.latestCompletedAtEpochMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ProgressSparkline(
                    points = exercise.trendPoints,
                    modifier = Modifier
                        .width(104.dp)
                        .height(44.dp),
                )
                Text(
                    text = "View chart",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun RangeSelector(
    selectedRange: ProgressRangeOption,
    onRangeSelected: (ProgressRangeOption) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProgressRangeOption.entries.forEach { range ->
            FilterChip(
                selected = range == selectedRange,
                onClick = { onRangeSelected(range) },
                label = { Text(range.label) },
            )
        }
    }
}

@Composable
private fun ExerciseProgressChartCard(points: List<ExerciseProgressPointUi>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Estimated 1RM",
                style = MaterialTheme.typography.titleMedium,
            )
            ProgressSparkline(
                points = points.map { it.estimatedOneRepMaxKg },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                showPoints = true,
                strokeWidthDp = 4.dp,
            )
            if (points.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatDate(points.first().completedAtEpochMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatDate(points.last().completedAtEpochMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    text = "No saved progress points for this range yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProgressSummaryMetrics(
    latestWeightKg: Double,
    latestReps: Int,
    bestEstimatedOneRepMaxKg: Double,
    totalSessions: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetricCard(
            label = "Latest",
            value = if (latestWeightKg > 0.0) "${formatWeight(latestWeightKg)} x $latestReps" else "-",
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            label = "Best 1RM",
            value = if (bestEstimatedOneRepMaxKg > 0.0) formatWeight(bestEstimatedOneRepMaxKg) else "-",
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            label = "Sessions",
            value = totalSessions.toString(),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ExerciseProgressPointCard(point: ExerciseProgressPointUi) {
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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatDate(point.completedAtEpochMs),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = CircleShape,
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "Session ${point.sessionId}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${formatWeight(point.weightKg)} x ${point.reps}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "Est. 1RM ${formatWeight(point.estimatedOneRepMaxKg)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProgressMessageCard(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    body: String? = null,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (!body.isNullOrBlank()) {
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
            if (!actionLabel.isNullOrBlank() && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun ProgressSparkline(
    points: List<Double>,
    modifier: Modifier = Modifier,
    showPoints: Boolean = false,
    strokeWidthDp: androidx.compose.ui.unit.Dp = 3.dp,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.primary
    val baselineColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier) {
        drawLine(
            color = baselineColor,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1.dp.toPx(),
        )

        if (points.isEmpty()) return@Canvas

        val max = points.maxOrNull() ?: 0.0
        val min = points.minOrNull() ?: max
        val range = if (abs(max - min) < 0.001) 1.0 else max - min
        val stepX = if (points.size == 1) 0f else size.width / (points.size - 1)
        val path = Path()
        val coordinates = points.mapIndexed { index, value ->
            val x = if (points.size == 1) size.width / 2f else index * stepX
            val normalized = ((value - min) / range).toFloat()
            val y = size.height - (normalized * size.height)
            Offset(x, y)
        }

        coordinates.forEachIndexed { index, offset ->
            if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = strokeWidthDp.toPx(), cap = StrokeCap.Round),
        )

        if (showPoints || coordinates.size == 1) {
            coordinates.forEach { offset ->
                drawCircle(
                    color = pointColor,
                    radius = if (showPoints) 5.dp.toPx() else 4.dp.toPx(),
                    center = offset,
                )
            }
        }
    }
}

private fun formatWeight(weightKg: Double): String =
    if (weightKg == weightKg.toLong().toDouble()) {
        "${weightKg.toLong()} kg"
    } else {
        "${"%.1f".format(weightKg)} kg"
    }

private fun formatDate(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("d MMM yyyy"))
