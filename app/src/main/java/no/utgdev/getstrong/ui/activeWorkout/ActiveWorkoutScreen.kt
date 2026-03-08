package no.utgdev.getstrong.ui.activeWorkout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.SessionSetType

@Composable
fun ActiveWorkoutScreen(
    uiState: ActiveWorkoutUiState,
    onCompleteSet: (Long, Int) -> Unit,
    onFocusSet: (Long) -> Unit,
    onFinishSession: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val completedCount = uiState.plannedSets.count { it.isCompleted }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Active Workout", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Session ID: ${uiState.sessionId}")
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
                    text = "Current Set",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = formatSet(currentSet),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Button(onClick = { onCompleteSet(currentSet.id, currentSet.targetReps) }) {
                    Text("Complete Current Set")
                }
            }
        } else {
            Text(text = "No pending sets")
        }

        Text(text = "Completed sets: $completedCount / ${uiState.plannedSets.size}")

        Text(text = "All Planned Sets", style = MaterialTheme.typography.titleMedium)
        uiState.plannedSets.forEach { set ->
            PlannedSetRow(
                set = set,
                isCurrent = uiState.currentSet?.id == set.id,
                isHighlighted = uiState.highlightedSetId == set.id,
                onFocusSet = onFocusSet,
                onCompleteSet = onCompleteSet,
            )
        }

        Button(onClick = onFinishSession, enabled = uiState.isCompleted) {
            Text("Finish Session")
        }

        Button(onClick = onExit) {
            Text("Exit Workout")
        }
    }
}

@Composable
private fun PlannedSetRow(
    set: SessionPlannedSet,
    isCurrent: Boolean,
    isHighlighted: Boolean,
    onFocusSet: (Long) -> Unit,
    onCompleteSet: (Long, Int) -> Unit,
) {
    val bgColor = when {
        set.isCompleted -> Color(0xFFDCFCE7)
        isCurrent -> MaterialTheme.colorScheme.secondaryContainer
        set.setType == SessionSetType.WARMUP -> Color(0xFFE0F2FE)
        isHighlighted -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val completionText = if (set.isCompleted) {
            "Completed (${set.completedReps ?: 0} reps)"
        } else {
            "Pending"
        }
        Text(text = "${setTypeLabel(set)} - ${formatSet(set)} - $completionText")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onFocusSet(set.id) }) {
                Text("Focus")
            }
            Button(
                onClick = { onCompleteSet(set.id, set.targetReps) },
                enabled = !set.isCompleted,
            ) {
                Text("Complete")
            }
        }
    }
}

private fun formatSet(set: SessionPlannedSet): String =
    "#${set.setOrder + 1} exercise=${set.exerciseId} target=${set.targetReps}${set.targetWeightKg?.let { " @${it}kg" } ?: ""}"

private fun setTypeLabel(set: SessionPlannedSet): String =
    if (set.setType == SessionSetType.WARMUP) "WARMUP" else "WORK"
