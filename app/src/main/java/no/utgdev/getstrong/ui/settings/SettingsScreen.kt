package no.utgdev.getstrong.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import no.utgdev.getstrong.domain.model.ProgressionModeCode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onRestDurationChanged: (String) -> Unit,
    onIncrementChanged: (String) -> Unit,
    onDeloadPercentChanged: (String) -> Unit,
    onProgressionModeChanged: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TrainingDefaultsIntroCard()
            }

            item {
                TrainingDefaultsFormCard(
                    uiState = uiState,
                    onRestDurationChanged = onRestDurationChanged,
                    onIncrementChanged = onIncrementChanged,
                    onDeloadPercentChanged = onDeloadPercentChanged,
                    onProgressionModeChanged = onProgressionModeChanged,
                )
            }

            if (uiState.feedbackMessage.isNotBlank()) {
                item {
                    FeedbackCard(
                        message = uiState.feedbackMessage,
                        isError = uiState.hasError,
                    )
                }
            }

            item {
                Button(
                    onClick = onSave,
                    enabled = uiState.isLoaded,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) {
                    Text("Save Training Defaults")
                }
            }
        }
    }
}

@Composable
private fun TrainingDefaultsIntroCard(
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Training defaults",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "These values prefill new sessions and new workout slots. Existing workout slots keep their saved configuration unless you edit them directly.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TrainingDefaultsFormCard(
    uiState: SettingsUiState,
    onRestDurationChanged: (String) -> Unit,
    onIncrementChanged: (String) -> Unit,
    onDeloadPercentChanged: (String) -> Unit,
    onProgressionModeChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "New slot defaults",
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = uiState.restDurationInput,
                onValueChange = onRestDurationChanged,
                label = { Text("Rest duration") },
                supportingText = { Text("Used when starting new sessions.") },
                suffix = { Text("sec") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = uiState.hasError && uiState.feedbackMessage.contains("Rest duration"),
            )

            OutlinedTextField(
                value = uiState.incrementInput,
                onValueChange = onIncrementChanged,
                label = { Text("Load increment") },
                supportingText = { Text("Prefills the next increment for new workout slots.") },
                suffix = { Text("kg") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = uiState.hasError && uiState.feedbackMessage.contains("Increment"),
            )

            OutlinedTextField(
                value = uiState.deloadInput,
                onValueChange = onDeloadPercentChanged,
                label = { Text("Deload percent") },
                supportingText = { Text("Used as the default deload fallback for new workout slots.") },
                suffix = { Text("%") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = uiState.hasError && uiState.feedbackMessage.contains("Deload"),
            )

            Text(
                text = "Default progression mode",
                style = MaterialTheme.typography.titleSmall,
            )

            ProgressionModeOptionRow(
                label = "Weight only",
                supportingText = "Increase load as the default behavior for new slots.",
                mode = ProgressionModeCode.WEIGHT_ONLY,
                selectedMode = uiState.progressionMode,
                onSelect = onProgressionModeChanged,
            )
            ProgressionModeOptionRow(
                label = "Reps only",
                supportingText = "Use rep increases as the default behavior for new slots.",
                mode = ProgressionModeCode.REPS_ONLY,
                selectedMode = uiState.progressionMode,
                onSelect = onProgressionModeChanged,
            )
            ProgressionModeOptionRow(
                label = "Reps then weight",
                supportingText = "Default new slots to progress reps first, then add load.",
                mode = ProgressionModeCode.REPS_THEN_WEIGHT,
                selectedMode = uiState.progressionMode,
                onSelect = onProgressionModeChanged,
            )
        }
    }
}

@Composable
private fun ProgressionModeOptionRow(
    label: String,
    supportingText: String,
    mode: String,
    selectedMode: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = mode == selectedMode,
                onClick = { onSelect(mode) },
                role = Role.RadioButton,
            )
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(
            selected = mode == selectedMode,
            onClick = null,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FeedbackCard(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    }
    val contentColor = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Text(
            text = message,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
        )
    }
}
