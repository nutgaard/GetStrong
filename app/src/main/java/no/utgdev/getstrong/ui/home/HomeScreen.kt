package no.utgdev.getstrong.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import no.utgdev.getstrong.ui.common.InlineStateCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onQuickStart: () -> Unit,
    onOpenPrograms: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasUpcomingWorkouts = uiState.upcomingWorkouts.isNotEmpty()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Home") },
            )
        },
        floatingActionButton = {
            if (hasUpcomingWorkouts) {
                ExtendedFloatingActionButton(
                    text = {
                        Text(
                            text = if (uiState.isStartingWorkout) "Starting..." else "Start Workout",
                        )
                    },
                    icon = {},
                    onClick = onQuickStart,
                    expanded = true,
                    modifier = Modifier.semantics {
                        contentDescription = buildQuickStartActionDescription(
                            nextWorkout = uiState.upcomingWorkouts.firstOrNull(),
                            isStarting = uiState.isStartingWorkout,
                        )
                    },
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Up next",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
            }

            if (uiState.startErrorMessage != null) {
                item {
                    InlineStateCard(
                        title = uiState.startErrorMessage,
                        actionLabel = "Try Again",
                        actionContentDescription = "Try starting the next workout again",
                        onAction = onQuickStart,
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    item {
                        InlineStateCard(
                            title = "Loading upcoming workouts...",
                            body = "Your saved workout queue will appear here once Home finishes loading.",
                        )
                    }
                }
                uiState.errorMessage != null -> {
                    item {
                        InlineStateCard(
                            title = uiState.errorMessage,
                            body = "Retry to rebuild the Home queue from your saved workouts.",
                            actionLabel = "Retry",
                            actionContentDescription = "Retry loading upcoming workouts on Home",
                            onAction = onRetry,
                        )
                    }
                }
                !uiState.hasSavedWorkouts -> {
                    item {
                        InlineStateCard(
                            title = "No workouts yet.",
                            body = "Create your first workout in Programs to build the upcoming queue.",
                            actionLabel = "Open Programs",
                            actionContentDescription = "Open Programs to create your first workout",
                            onAction = onOpenPrograms,
                        )
                    }
                }
                !hasUpcomingWorkouts -> {
                    item {
                        InlineStateCard(
                            title = "No ready-to-start workouts yet.",
                            body = "Add exercises to your saved workouts in Programs to populate the upcoming queue.",
                            actionLabel = "Open Programs",
                            actionContentDescription = "Open Programs to add exercises to your saved workouts",
                            onAction = onOpenPrograms,
                        )
                    }
                }
                else -> {
                    items(uiState.upcomingWorkouts, key = { it.workoutId }) { workout ->
                        UpcomingWorkoutCard(workout = workout)
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingWorkoutCard(
    workout: HomeUpcomingWorkoutUi,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = buildUpcomingWorkoutCardDescription(workout)
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = workout.workoutName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = workout.scheduledLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (workout.isNextUp) {
                    QueueBadge(text = "Next up")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                workout.exercisePreview.forEach { exerciseName ->
                    Text(
                        text = "- $exerciseName",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (workout.additionalExerciseCount > 0) {
                    Text(
                        text = "+${workout.additionalExerciseCount} more exercises",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueBadge(
    text: String,
) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

internal fun buildQuickStartActionDescription(
    nextWorkout: HomeUpcomingWorkoutUi?,
    isStarting: Boolean,
): String =
    when {
        nextWorkout == null -> "Start workout"
        isStarting -> "Starting ${nextWorkout.workoutName}"
        else -> "Start workout ${nextWorkout.workoutName}"
    }

internal fun buildUpcomingWorkoutCardDescription(workout: HomeUpcomingWorkoutUi): String =
    buildString {
        append(workout.workoutName)
        append(". Scheduled ${workout.scheduledLabel}.")
        if (workout.isNextUp) {
            append(" Next up.")
        }
        if (workout.exercisePreview.isNotEmpty()) {
            append(" Upcoming lifts: ${workout.exercisePreview.joinToString()}.")
        }
        if (workout.additionalExerciseCount > 0) {
            append(" Plus ${workout.additionalExerciseCount} more exercises.")
        }
    }
