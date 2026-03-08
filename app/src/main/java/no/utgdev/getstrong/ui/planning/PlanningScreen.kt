package no.utgdev.getstrong.ui.planning

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
fun PlanningScreen(
    onBack: () -> Unit,
    onStartWorkout: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Workout Planning", style = MaterialTheme.typography.headlineMedium)
        Text("Your saved workouts will appear here.")

        Button(onClick = { onStartWorkout("sample-workout") }) {
            Text("Start Sample Workout")
        }

        Button(onClick = onBack) {
            Text("Back to Home")
        }
    }
}
