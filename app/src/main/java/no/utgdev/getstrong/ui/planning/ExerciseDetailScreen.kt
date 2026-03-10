package no.utgdev.getstrong.ui.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import no.utgdev.getstrong.domain.model.ProgressionModeCode
import no.utgdev.getstrong.ui.common.InlineStateCard

private enum class ExerciseDetailSection(val title: String) {
    WEIGHT("Weight"),
    PROGRESS("Progress"),
    HISTORY("History"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    detail: ExerciseSlotDetailUi?,
    onBack: () -> Unit,
    onSave: (
        targetSets: Int,
        targetReps: Int,
        currentWorkingWeightKg: Double,
        progressionMode: String,
        incrementKg: Double,
        deloadPercent: Int,
    ) -> Unit,
    onOpenProgress: (Long) -> Unit,
    onOpenHistory: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedSection by rememberSaveable { mutableStateOf(ExerciseDetailSection.WEIGHT) }
    var targetSetsInput by rememberSaveable(detail?.exerciseId) { mutableStateOf(detail?.targetSets?.toString().orEmpty()) }
    var targetRepsInput by rememberSaveable(detail?.exerciseId) { mutableStateOf(detail?.targetReps?.toString().orEmpty()) }
    var weightInput by rememberSaveable(detail?.exerciseId) { mutableStateOf(detail?.currentWorkingWeightKg?.toString().orEmpty()) }
    var incrementInput by rememberSaveable(detail?.exerciseId) { mutableStateOf(detail?.incrementKg?.toString().orEmpty()) }
    var deloadInput by rememberSaveable(detail?.exerciseId) { mutableStateOf(detail?.deloadPercent?.toString().orEmpty()) }
    var progressionMode by rememberSaveable(detail?.exerciseId) {
        mutableStateOf(detail?.progressionMode ?: ProgressionModeCode.WEIGHT_ONLY)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(detail?.exerciseName ?: "Exercise Detail") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (detail == null) {
                item {
                    InlineStateCard(
                        title = "Could not load exercise detail.",
                        body = "Return to workout editor and select an exercise again.",
                    )
                }
                return@LazyColumn
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ExerciseDetailSection.entries.forEach { section ->
                        FilterChip(
                            selected = selectedSection == section,
                            onClick = {
                                selectedSection = section
                                when (section) {
                                    ExerciseDetailSection.PROGRESS -> onOpenProgress(detail.exerciseId)
                                    ExerciseDetailSection.HISTORY -> onOpenHistory(detail.exerciseId)
                                    ExerciseDetailSection.WEIGHT -> Unit
                                }
                            },
                            label = { Text(section.title) },
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Form is intentionally hidden in this slice.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                Text(
                    text = "${detail.exerciseName} (${detail.equipmentType.ifBlank { "Unknown equipment" }})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            value = targetSetsInput,
                            onValueChange = { targetSetsInput = it },
                            label = { Text("Sets") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = targetRepsInput,
                            onValueChange = { targetRepsInput = it },
                            label = { Text("Reps") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text("Working weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "Progression",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            progressionModeOptions().forEach { mode ->
                                FilterChip(
                                    selected = progressionMode == mode,
                                    onClick = { progressionMode = mode },
                                    label = { Text(modeLabel(mode)) },
                                )
                            }
                        }
                        OutlinedTextField(
                            value = incrementInput,
                            onValueChange = { incrementInput = it },
                            label = { Text("Increment (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = deloadInput,
                            onValueChange = { deloadInput = it },
                            label = { Text("Deload (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = detail.plateGuidance,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(
                            onClick = {
                                val targetSets = targetSetsInput.toIntOrNull() ?: detail.targetSets
                                val targetReps = targetRepsInput.toIntOrNull() ?: detail.targetReps
                                val weight = weightInput.toDoubleOrNull() ?: detail.currentWorkingWeightKg
                                val increment = incrementInput.toDoubleOrNull() ?: detail.incrementKg
                                val deload = deloadInput.toIntOrNull() ?: detail.deloadPercent
                                onSave(
                                    targetSets,
                                    targetReps,
                                    weight,
                                    progressionMode,
                                    increment,
                                    deload,
                                )
                            },
                        ) {
                            Text("Apply Slot Settings")
                        }
                    }
                }
            }
        }
    }
}

private fun progressionModeOptions(): List<String> = listOf(
    ProgressionModeCode.WEIGHT_ONLY,
    ProgressionModeCode.REPS_ONLY,
    ProgressionModeCode.REPS_THEN_WEIGHT,
)

private fun modeLabel(mode: String): String =
    when (mode) {
        ProgressionModeCode.WEIGHT_ONLY -> "Weight"
        ProgressionModeCode.REPS_ONLY -> "Reps"
        ProgressionModeCode.REPS_THEN_WEIGHT -> "Reps→Weight"
        else -> mode
    }
