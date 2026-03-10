package no.utgdev.getstrong.ui.summary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import no.utgdev.getstrong.domain.model.SessionSetType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    uiState: SummaryUiState,
    onDone: () -> Unit,
    onRetryLoad: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDone)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Workout Summary") },
            )
        },
        bottomBar = {
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .heightIn(min = 52.dp)
                    .semantics { contentDescription = "Dismiss workout summary and return to the app" },
            ) {
                Text("Return To App")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                uiState.isLoading -> {
                    item {
                        SummaryMessageCard(
                            title = "Loading summary",
                            body = "Pulling the final session totals and set results.",
                        )
                    }
                }
                uiState.errorMessage != null -> {
                    item {
                        SummaryMessageCard(
                            title = "Summary unavailable",
                            body = uiState.errorMessage,
                            actionLabel = "Retry",
                            onAction = onRetryLoad,
                        )
                    }
                }
                uiState.sets.isEmpty() -> {
                    item {
                        SummaryMessageCard(
                            title = "No summary data",
                            body = "This session does not have any persisted set results yet.",
                        )
                    }
                }
                else -> {
                    item {
                        SessionTotalsSection(uiState = uiState)
                    }
                    item {
                        Text(
                            text = "Set Results",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                    items(uiState.sets, key = { "${it.setOrder}-${it.exerciseId}-${it.setType}" }) { row ->
                        SummarySetCard(row = row)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMessageCard(
    title: String,
    body: String,
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
            Text(body, style = MaterialTheme.typography.bodyMedium)
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun SessionTotalsSection(uiState: SummaryUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Session ${uiState.sessionId}", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = "Final review",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SummaryMetricCard(
                label = "Total Time",
                value = formatElapsed(uiState.totalDurationSeconds),
                modifier = Modifier.weight(1f),
            )
            SummaryMetricCard(
                label = "Total Volume",
                value = formatWeight(uiState.totalVolumeKg),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun SummarySetCard(row: SummarySetRowUi) {
    val isWarmup = row.setType == SessionSetType.WARMUP
    val containerColor = if (isWarmup) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (isWarmup) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(containerColor)
                .padding(16.dp)
                .semantics {
                    contentDescription = buildString {
                        append("${if (isWarmup) "Warmup" else "Work"} set ${row.setOrder + 1}. ")
                        append("${row.exerciseName}. ")
                        append("Target ${row.targetReps} reps. ")
                        append("Achieved ${row.achievedReps ?: 0} reps. ")
                        append("Load ${row.loadKg?.let(::formatWeight) ?: "not recorded"}")
                    }
                },
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = row.exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor,
                    )
                    Text(
                        text = "Set ${row.setOrder + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor,
                    )
                }
                Text(
                    text = if (isWarmup) "WARMUP" else "WORK",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryValueBlock(
                    label = "Target",
                    value = "${row.targetReps} reps",
                    color = contentColor,
                    modifier = Modifier.weight(1f),
                )
                SummaryValueBlock(
                    label = "Achieved",
                    value = "${row.achievedReps ?: 0} reps",
                    color = contentColor,
                    modifier = Modifier.weight(1f),
                )
                SummaryValueBlock(
                    label = "Load",
                    value = row.loadKg?.let(::formatWeight) ?: "-",
                    color = contentColor,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SummaryValueBlock(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

private fun formatElapsed(seconds: Long): String {
    val total = seconds.coerceAtLeast(0L)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val secs = total % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, secs)
    } else {
        "%02d:%02d".format(minutes, secs)
    }
}

private fun formatWeight(weightKg: Double): String =
    if (weightKg % 1.0 == 0.0) {
        "${weightKg.toInt()} kg"
    } else {
        "${"%.1f".format(weightKg)} kg"
    }
