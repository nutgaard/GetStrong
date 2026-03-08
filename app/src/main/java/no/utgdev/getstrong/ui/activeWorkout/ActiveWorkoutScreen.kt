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
                        .heightIn(min = 56.dp),
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
                            .heightIn(min = 52.dp),
                    ) {
                        Text("Finish")
                    }
                    Button(
                        onClick = onExit,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
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
            Text(text = "Active Workout", style = MaterialTheme.typography.headlineMedium)
            when {
                uiState.isRestTimerActive -> {
                    Text(
                        text = "Rest: ${uiState.restRemainingSeconds}s",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                uiState.isRestOver -> {
                    Text(
                        text = "Rest over",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF16A34A),
                    )
                }
            }

            val currentSet = uiState.currentSet
            if (currentSet != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                        .padding(12.dp),
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
            )
            Text(text = "Pending Sets", style = MaterialTheme.typography.titleMedium)
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
        set.setType == SessionSetType.WARMUP -> Color(0xFFE0F2FE)
        else -> MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "${setTypeLabel(set)} • ${formatSet(set)}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onCompleteSet(set.id, set.targetReps) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
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
