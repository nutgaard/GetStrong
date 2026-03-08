package no.utgdev.getstrong.ui.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import no.utgdev.getstrong.domain.model.Workout

@Composable
fun PlanningScreen(
    uiState: PlanningUiState,
    onBack: () -> Unit,
    onCreateWorkout: () -> Unit,
    onRetryLoad: () -> Unit,
    onEditWorkout: (Long) -> Unit,
    onDeleteWorkout: (Long) -> Unit,
    onStartWorkout: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onCreateWorkout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp)
                        .semantics { contentDescription = "Create workout" },
                ) {
                    Text("Create Workout")
                }
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .semantics { contentDescription = "Back to home" },
                ) {
                    Text("Back to Home")
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Workout Planning",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )

            when {
                uiState.isLoading -> {
                    Text("Loading workouts...")
                }
                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(
                        onClick = onRetryLoad,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Text("Retry")
                    }
                }
                uiState.workouts.isEmpty() -> {
                    Text(
                        text = "No workouts yet. Create one to get started.",
                        modifier = Modifier.semantics { contentDescription = "No workouts yet. Create one to get started." },
                    )
                    Button(
                        onClick = onCreateWorkout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Text("Create Workout")
                    }
                }
                else -> {
                    uiState.workouts.forEach { workout ->
                        WorkoutRow(
                            workout = workout,
                            onEditWorkout = onEditWorkout,
                            onDeleteWorkout = onDeleteWorkout,
                            onStartWorkout = onStartWorkout,
                        )
                    }
                }
            }
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics { contentDescription = "Workout ${workout.name}. ${workout.slots.size} exercises." },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = workout.name, style = MaterialTheme.typography.titleMedium)
        Text(text = "Exercises: ${workout.slots.size}")
        Button(
            onClick = { onStartWorkout(workout.id) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .semantics { contentDescription = "Start workout ${workout.name}" },
        ) {
            Text("Start Workout")
        }
        Button(
            onClick = { onEditWorkout(workout.id) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics { contentDescription = "Edit workout ${workout.name}" },
        ) {
            Text("Edit Workout")
        }
        Button(
            onClick = { onDeleteWorkout(workout.id) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics { contentDescription = "Delete workout ${workout.name}" },
        ) {
            Text("Delete Workout")
        }
    }
}
