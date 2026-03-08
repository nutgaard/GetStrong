package no.utgdev.getstrong.ui.activeWorkout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.SessionSetType

@Suppress("UNUSED_PARAMETER")
@Composable
fun ActiveWorkoutScreen(
    uiState: ActiveWorkoutUiState,
    onCompleteSet: (Long, Int) -> Unit,
    onFocusSet: (Long) -> Unit,
    onFinishSession: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KeepScreenOnEffect(enabled = uiState.isSessionActive)
    val completedCount = uiState.plannedSets.count { it.isCompleted }
    val pendingSets = uiState.plannedSets.filter { !it.isCompleted }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val currentSet = uiState.currentSet
                Button(
                    onClick = {
                        if (currentSet != null) {
                            onCompleteSet(currentSet.id, currentSet.targetReps)
                        }
                    },
                    enabled = currentSet != null && !uiState.isRestTimerActive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .semantics {
                            contentDescription = "Complete current set"
                            stateDescription = when {
                                currentSet == null -> "No set available"
                                uiState.isRestTimerActive -> "Disabled during rest timer"
                                else -> "Enabled"
                            }
                        },
                ) {
                    Text("Complete Current Set")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onFinishSession,
                        enabled = uiState.isCompleted,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .semantics {
                                contentDescription = "Finish workout session"
                                stateDescription = if (uiState.isCompleted) "Enabled" else "Disabled until all sets complete"
                            },
                    ) {
                        Text("Finish")
                    }
                    Button(
                        onClick = onExit,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .semantics { contentDescription = "Exit active workout" },
                    ) {
                        Text("Exit")
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Active Workout",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = "Elapsed ${formatElapsed(uiState.elapsedSeconds)}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { contentDescription = "Elapsed time ${formatElapsed(uiState.elapsedSeconds)}" },
            )
            when {
                uiState.isRestTimerActive -> {
                    Text(
                        text = "Rest: ${uiState.restRemainingSeconds}s",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { contentDescription = "Rest timer ${uiState.restRemainingSeconds} seconds remaining" },
                    )
                }
                uiState.isRestOver -> {
                    Text(
                        text = "Rest over",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { contentDescription = "Rest over" },
                    )
                }
            }

            val currentSet = uiState.currentSet
            if (currentSet != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                        .semantics {
                            contentDescription = "Current set ${setTypeLabel(currentSet)}. ${formatCurrentSet(currentSet)}"
                            stateDescription = if (currentSet.isCompleted) "Completed" else "Pending"
                        },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Current Set • ${setTypeLabel(currentSet)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = formatCurrentSet(currentSet),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            } else {
                Text(text = "No pending sets")
            }

            Text(
                text = "Progress: $completedCount / ${uiState.plannedSets.size}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics {
                    contentDescription = "Progress $completedCount of ${uiState.plannedSets.size} sets completed"
                },
            )
            Text(
                text = "Pending Sets",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            pendingSets.forEach { set ->
                PlannedSetRow(
                    set = set,
                    isCurrent = uiState.currentSet?.id == set.id,
                    onCompleteSet = onCompleteSet,
                )
            }
            Spacer(modifier = Modifier.padding(bottom = 8.dp))
        }
    }
}

@Composable
private fun PlannedSetRow(
    set: SessionPlannedSet,
    isCurrent: Boolean,
    onCompleteSet: (Long, Int) -> Unit,
) {
    val bgColor = when {
        isCurrent -> MaterialTheme.colorScheme.secondaryContainer
        set.setType == SessionSetType.WARMUP -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(10.dp))
            .padding(10.dp)
            .semantics {
                contentDescription = "${setTypeLabel(set)} set ${set.setOrder + 1}. ${formatSet(set)}"
                stateDescription = if (set.isCompleted) "Completed" else "Pending"
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "${setTypeLabel(set)} • ${formatSet(set)}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onCompleteSet(set.id, set.targetReps) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = "Complete ${setTypeLabel(set)} set ${set.setOrder + 1}" },
            ) {
                Text("Complete")
            }
        }
    }
}

private fun formatCurrentSet(set: SessionPlannedSet): String =
    "Exercise ${set.exerciseId} • target ${set.targetReps}${set.targetWeightKg?.let { " @${it}kg" } ?: ""}"

private fun formatSet(set: SessionPlannedSet): String =
    "#${set.setOrder + 1} ex ${set.exerciseId} • ${set.targetReps}${set.targetWeightKg?.let { " @${it}kg" } ?: ""}"

private fun setTypeLabel(set: SessionPlannedSet): String =
    if (set.setType == SessionSetType.WARMUP) "WARMUP" else "WORK"

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

@Composable
private fun KeepScreenOnEffect(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        val previous = view.keepScreenOn
        if (enabled) {
            view.keepScreenOn = true
        }
        onDispose {
            view.keepScreenOn = previous
        }
    }
}
