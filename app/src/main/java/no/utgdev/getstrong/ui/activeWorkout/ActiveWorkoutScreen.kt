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
    workoutId: String,
    onComplete: (String) -> Unit,
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
        Text(text = "Workout ID: $workoutId")

        Button(onClick = { onComplete("session-$workoutId") }) {
            Text("Complete Session")
        }

        Button(onClick = onExit) {
            Text("Exit Workout")
        }
    }
}
