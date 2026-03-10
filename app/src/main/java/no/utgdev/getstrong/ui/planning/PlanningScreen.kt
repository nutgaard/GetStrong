package no.utgdev.getstrong.ui.planning

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import no.utgdev.getstrong.domain.model.Workout
import no.utgdev.getstrong.ui.common.InlineStateCard

@Composable
fun PlanningScreen(
    uiState: PlanningUiState,
    onCreateWorkout: () -> Unit,
    onRetryLoad: () -> Unit,
    onEditWorkout: (Long) -> Unit,
    onDeleteWorkout: (Long) -> Unit,
    onStartWorkout: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateWorkout) {
                Text("Add")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Programs",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = "Workouts",
                style = MaterialTheme.typography.titleMedium,
            )
            when {
                uiState.isLoading -> {
                    InlineStateCard(
                        title = "Loading workouts...",
                        body = "Your saved workouts will appear here once loading finishes.",
                    )
                }
                uiState.errorMessage != null -> {
                    InlineStateCard(
                        title = "Couldn't load your workouts.",
                        body = "Try again to reload your Programs workout list.",
                        actionLabel = "Retry",
                        onAction = onRetryLoad,
                    )
                }
                uiState.workouts.isEmpty() -> {
                    InlineStateCard(
                        title = "No workouts yet.",
                        body = "Create your first workout to start training from Programs.",
                        actionLabel = "Create Workout",
                        onAction = onCreateWorkout,
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(uiState.workouts, key = { it.id }) { workout ->
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
}

@Composable
private fun WorkoutRow(
    workout: Workout,
    onEditWorkout: (Long) -> Unit,
    onDeleteWorkout: (Long) -> Unit,
    onStartWorkout: (Long) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditWorkout(workout.id) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = workout.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${workout.slots.size} exercises",
                style = MaterialTheme.typography.bodyMedium,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
            ) {
                TextButton(onClick = { menuExpanded = true }) {
                    Text("Actions")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Start workout") },
                        onClick = {
                            menuExpanded = false
                            onStartWorkout(workout.id)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Edit workout") },
                        onClick = {
                            menuExpanded = false
                            onEditWorkout(workout.id)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete workout") },
                        onClick = {
                            menuExpanded = false
                            onDeleteWorkout(workout.id)
                        },
                    )
                }
            }
        }
    }
}
