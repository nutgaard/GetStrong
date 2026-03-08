package no.utgdev.getstrong.ui.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import no.utgdev.getstrong.domain.model.Workout

@Composable
fun PlanningScreen(
    uiState: PlanningUiState,
    onBack: () -> Unit,
    onCreateWorkout: () -> Unit,
    onEditWorkout: (Long) -> Unit,
    onDeleteWorkout: (Long) -> Unit,
    onStartWorkout: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Workout Planning", style = MaterialTheme.typography.headlineMedium)

        Button(onClick = onCreateWorkout) {
            Text("Create Workout")
        }

        uiState.workouts.forEach { workout ->
            WorkoutRow(
                workout = workout,
                onEditWorkout = onEditWorkout,
                onDeleteWorkout = onDeleteWorkout,
                onStartWorkout = onStartWorkout,
            )
        }

        Button(onClick = onBack) {
            Text("Back to Home")
        }
    }
}

@Composable
private fun WorkoutRow(
    workout: Workout,
    onEditWorkout: (Long) -> Unit,
    onDeleteWorkout: (Long) -> Unit,
    onStartWorkout: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = workout.name, style = MaterialTheme.typography.titleMedium)
        Text(text = "Exercises: ${workout.slots.size}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onEditWorkout(workout.id) }) {
                Text("Edit")
            }
            Button(onClick = { onDeleteWorkout(workout.id) }) {
                Text("Delete")
            }
            Button(onClick = { onStartWorkout(workout.id) }) {
                Text("Start")
            }
        }
    }
}
