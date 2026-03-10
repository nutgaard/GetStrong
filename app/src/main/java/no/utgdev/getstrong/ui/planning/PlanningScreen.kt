package no.utgdev.getstrong.ui.planning

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import no.utgdev.getstrong.domain.model.Workout
import no.utgdev.getstrong.ui.common.InlineStateCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningScreen(
    uiState: PlanningUiState,
    onCreateWorkout: () -> Unit,
    onRetryLoad: () -> Unit,
    onEditWorkout: (Long) -> Unit,
    onDeleteWorkout: (Long) -> Unit,
    onStartWorkout: (Long) -> Unit,
    onToggleTrainingDay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Programs") },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateWorkout,
                modifier = Modifier
                    .planningFabInset()
                    .semantics {
                    contentDescription = "Create new workout"
                },
            ) {
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
                text = "Workouts",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
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
                        actionContentDescription = "Retry loading workouts in Programs",
                        onAction = onRetryLoad,
                    )
                }
                uiState.workouts.isEmpty() -> {
                    InlineStateCard(
                        title = "No workouts yet.",
                        body = "Create your first workout, then set training days below.",
                        actionLabel = "Create Workout",
                        actionContentDescription = "Create your first workout from Programs",
                        onAction = onCreateWorkout,
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
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
            Text(
                text = "Schedule",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            WeeklyScheduleEditor(
                selectedDays = uiState.trainingDays,
                onToggleTrainingDay = onToggleTrainingDay,
            )
            if (uiState.scheduleErrorMessage != null) {
                InlineStateCard(
                    title = uiState.scheduleErrorMessage,
                )
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
    var shouldRestoreActionFocus by remember { mutableStateOf(false) }
    val actionFocusRequester = remember { FocusRequester() }

    LaunchedEffect(menuExpanded, shouldRestoreActionFocus) {
        if (!menuExpanded && shouldRestoreActionFocus) {
            actionFocusRequester.requestFocus()
            shouldRestoreActionFocus = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = buildWorkoutRowContentDescription(workout)
            }
            .clickable(
                onClickLabel = "Edit workout ${workout.name}",
            ) { onEditWorkout(workout.id) },
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
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .focusRequester(actionFocusRequester)
                        .focusable()
                        .semantics {
                            contentDescription = "Open workout actions for ${workout.name}"
                        },
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_more),
                        contentDescription = null,
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = {
                        menuExpanded = false
                        shouldRestoreActionFocus = true
                    },
                ) {
                    DropdownMenuItem(
                        text = { Text("Start workout") },
                        modifier = Modifier.semantics {
                            contentDescription = "Start workout ${workout.name}"
                        },
                        onClick = {
                            menuExpanded = false
                            shouldRestoreActionFocus = true
                            onStartWorkout(workout.id)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Edit workout") },
                        modifier = Modifier.semantics {
                            contentDescription = "Edit workout ${workout.name}"
                        },
                        onClick = {
                            menuExpanded = false
                            shouldRestoreActionFocus = true
                            onEditWorkout(workout.id)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete workout") },
                        modifier = Modifier.semantics {
                            contentDescription = "Delete workout ${workout.name}"
                        },
                        onClick = {
                            menuExpanded = false
                            shouldRestoreActionFocus = true
                            onDeleteWorkout(workout.id)
                        },
                    )
                }
            }
        }
    }
}

internal fun buildWorkoutRowContentDescription(workout: Workout): String {
    val exerciseCount = workout.slots.size
    val exerciseSummary = when (exerciseCount) {
        0 -> "No exercises yet."
        1 -> "1 exercise."
        else -> "$exerciseCount exercises."
    }
    return "${workout.name}. $exerciseSummary"
}

@Composable
private fun WeeklyScheduleEditor(
    selectedDays: List<Int>,
    onToggleTrainingDay: (Int) -> Unit,
) {
    val mondayToSunday = listOf(
        1 to "Mon",
        2 to "Tue",
        3 to "Wed",
        4 to "Thu",
        5 to "Fri",
        6 to "Sat",
        7 to "Sun",
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            mondayToSunday.take(4).forEach { (day, label) ->
                FilterChip(
                    selected = selectedDays.contains(day),
                    onClick = { onToggleTrainingDay(day) },
                    label = { Text(label) },
                    modifier = Modifier.semantics {
                        contentDescription = "Toggle training day $label"
                    },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            mondayToSunday.drop(4).forEach { (day, label) ->
                FilterChip(
                    selected = selectedDays.contains(day),
                    onClick = { onToggleTrainingDay(day) },
                    label = { Text(label) },
                    modifier = Modifier.semantics {
                        contentDescription = "Toggle training day $label"
                    },
                )
            }
        }
    }
}
