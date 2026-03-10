package no.utgdev.getstrong.ui.planning

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import no.utgdev.getstrong.domain.model.Exercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutEditorScreen(
    uiState: WorkoutEditorUiState,
    onNameChanged: (String) -> Unit,
    onAddExercise: (Long) -> Unit,
    onRemoveSlot: (Long, Int) -> Unit,
    onMoveSlotUp: (Int) -> Unit,
    onMoveSlotDown: (Int) -> Unit,
    onUpdateSlotTargets: (Int, Int, Int) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showExercisePicker by rememberSaveable { mutableStateOf(false) }
    var editingPosition by rememberSaveable { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showExercisePicker = true },
                modifier = Modifier.planningFabInset(),
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
            val title = if (uiState.workoutId == null) "Create Workout" else "Edit Workout"
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )

            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChanged,
                label = { Text("Workout Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            uiState.message?.let { message ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(message, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onClearMessage) { Text("Dismiss") }
                    }
                }
            }

            Text(text = "Exercises", style = MaterialTheme.typography.titleMedium)
            if (uiState.slots.isEmpty()) {
                Text("No exercises yet. Use Add to pick from your catalog.")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.slots, key = { "${it.id}-${it.position}-${it.exerciseId}" }) { slot ->
                        WorkoutSlotRow(
                            slot = slot,
                            canMoveUp = slot.position > 0,
                            canMoveDown = slot.position < uiState.slots.lastIndex,
                            onEdit = { editingPosition = slot.position },
                            onMoveUp = { onMoveSlotUp(slot.position) },
                            onMoveDown = { onMoveSlotDown(slot.position) },
                            onRemove = { onRemoveSlot(slot.id, slot.position) },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                ) { Text("Save") }
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                ) { Text("Back") }
            }
        }
    }

    if (showExercisePicker) {
        ExercisePickerDialog(
            availableExercises = uiState.availableExercises,
            selectedExerciseIds = uiState.slots.map { it.exerciseId }.toSet(),
            onDismiss = { showExercisePicker = false },
            onSelectExercise = { exerciseId ->
                onAddExercise(exerciseId)
                showExercisePicker = false
            },
        )
    }

    val editingSlot = uiState.slots.firstOrNull { it.position == editingPosition }
    if (editingSlot != null) {
        SlotTargetsDialog(
            slot = editingSlot,
            onDismiss = { editingPosition = null },
            onSave = { sets, reps ->
                onUpdateSlotTargets(editingSlot.position, sets, reps)
                editingPosition = null
            },
        )
    }
}

@Composable
private fun WorkoutSlotRow(
    slot: WorkoutSlotDraft,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "${slot.position + 1}. ${slot.exerciseName}", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${slot.targetSets} sets x ${slot.targetReps} reps",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_more),
                        contentDescription = "Open exercise actions for ${slot.exerciseName}",
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit sets/reps") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Move up") },
                        enabled = canMoveUp,
                        onClick = {
                            showMenu = false
                            onMoveUp()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Move down") },
                        enabled = canMoveDown,
                        onClick = {
                            showMenu = false
                            onMoveDown()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete exercise") },
                        onClick = {
                            showMenu = false
                            onRemove()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExercisePickerDialog(
    availableExercises: List<Exercise>,
    selectedExerciseIds: Set<Long>,
    onDismiss: () -> Unit,
    onSelectExercise: (Long) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val remaining = remember(availableExercises, selectedExerciseIds, query) {
        val base = availableExercises.filterNot { selectedExerciseIds.contains(it.id) }
        if (query.isBlank()) {
            base
        } else {
            val lowered = query.trim().lowercase()
            base.filter { exercise ->
                exercise.name.lowercase().contains(lowered) ||
                    exercise.primaryMuscleGroup.lowercase().contains(lowered)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search exercises") },
                    singleLine = true,
                )
                if (remaining.isEmpty()) {
                    Text("No exercises available to add.")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(remaining, key = { it.id }) { exercise ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectExercise(exercise.id) },
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                ) {
                                    Text(text = exercise.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = exercise.primaryMuscleGroup,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun SlotTargetsDialog(
    slot: WorkoutSlotDraft,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit,
) {
    var setsInput by rememberSaveable(slot.position) { mutableStateOf(slot.targetSets.toString()) }
    var repsInput by rememberSaveable(slot.position) { mutableStateOf(slot.targetReps.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${slot.exerciseName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = setsInput,
                    onValueChange = { setsInput = it },
                    label = { Text("Sets") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = repsInput,
                    onValueChange = { repsInput = it },
                    label = { Text("Reps") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val sets = setsInput.toIntOrNull() ?: slot.targetSets
                    val reps = repsInput.toIntOrNull() ?: slot.targetReps
                    onSave(sets, reps)
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
