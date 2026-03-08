package no.utgdev.getstrong.ui.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WorkoutEditorScreen(
    uiState: WorkoutEditorUiState,
    onNameChanged: (String) -> Unit,
    onAddExercise: (Long) -> Unit,
    onRemoveSlot: (Long, Int) -> Unit,
    onMoveSlotUp: (Int) -> Unit,
    onMoveSlotDown: (Int) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val title = if (uiState.workoutId == null) "Create Workout" else "Edit Workout"
        Text(text = title, style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = uiState.name,
            onValueChange = onNameChanged,
            label = { Text("Workout Name") },
            modifier = Modifier.fillMaxWidth(),
        )

        Text(text = "Add Exercise", style = MaterialTheme.typography.titleMedium)
        uiState.availableExercises.forEach { exercise ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "${exercise.name} (${exercise.primaryMuscleGroup})")
                Button(onClick = { onAddExercise(exercise.id) }) {
                    Text("Add")
                }
            }
        }

        Text(text = "Workout Slots", style = MaterialTheme.typography.titleMedium)
        uiState.slots.forEach { slot ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "#${slot.position + 1} ${slot.exerciseName}")
                Text(text = "${slot.targetSets}x${slot.targetReps} ${slot.progressionMode}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onMoveSlotUp(slot.position) }, enabled = slot.position > 0) {
                        Text("Up")
                    }
                    Button(
                        onClick = { onMoveSlotDown(slot.position) },
                        enabled = slot.position < uiState.slots.lastIndex,
                    ) {
                        Text("Down")
                    }
                    Button(onClick = { onRemoveSlot(slot.id, slot.position) }) {
                        Text("Remove")
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave) {
                Text("Save")
            }
            Button(onClick = onBack) {
                Text("Back")
            }
        }
    }
}
