package no.utgdev.getstrong.ui.activeWorkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ActiveWorkoutScreen(
    uiState: ActiveWorkoutUiState,
    onCompleteSet: () -> Unit,
    onFinishSession: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Active Workout", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Session ID: ${uiState.sessionId}")

        val currentSet = uiState.currentSet
        if (currentSet != null) {
            Text(text = "Current set: ${currentSet.setType} - exercise ${currentSet.exerciseId} - reps ${currentSet.targetReps}")
            Button(onClick = onCompleteSet) {
                Text("Complete Current Set")
            }
        } else {
            Text(text = "No pending sets")
        }

        Text(text = "Completed sets: ${uiState.plannedSets.count { it.isCompleted }} / ${uiState.plannedSets.size}")

        Button(onClick = onFinishSession, enabled = uiState.isCompleted) {
            Text("Finish Session")
        }

        Button(onClick = onExit) {
            Text("Exit Workout")
        }
    }
}
