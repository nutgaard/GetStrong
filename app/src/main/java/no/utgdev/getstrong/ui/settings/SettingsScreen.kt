package no.utgdev.getstrong.ui.settings

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import no.utgdev.getstrong.domain.model.ProgressionModeCode

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
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = uiState.restDurationInput,
            onValueChange = onRestDurationChanged,
            label = { Text("Default rest duration (sec)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = uiState.incrementInput,
            onValueChange = onIncrementChanged,
            label = { Text("Default increment (kg)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = uiState.deloadInput,
            onValueChange = onDeloadPercentChanged,
            label = { Text("Default deload percent") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Text(text = "Default progression mode", style = MaterialTheme.typography.titleMedium)
        ProgressionModeOption(
            label = "Weight only",
            mode = ProgressionModeCode.WEIGHT_ONLY,
            selectedMode = uiState.progressionMode,
            onSelect = onProgressionModeChanged,
        )
        ProgressionModeOption(
            label = "Reps only",
            mode = ProgressionModeCode.REPS_ONLY,
            selectedMode = uiState.progressionMode,
            onSelect = onProgressionModeChanged,
        )
        ProgressionModeOption(
            label = "Reps then weight",
            mode = ProgressionModeCode.REPS_THEN_WEIGHT,
            selectedMode = uiState.progressionMode,
            onSelect = onProgressionModeChanged,
        )

        if (uiState.feedbackMessage.isNotBlank()) {
            Text(
                text = uiState.feedbackMessage,
                color = if (uiState.hasError) Color(0xFFB91C1C) else Color(0xFF166534),
            )
        }

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Text("Save Settings")
        }
    }
}

@Composable
private fun ProgressionModeOption(
    label: String,
    mode: String,
    selectedMode: String,
    onSelect: (String) -> Unit,
) {
    Button(
        onClick = { onSelect(mode) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        val selected = if (mode == selectedMode) " (selected)" else ""
        Text("$label$selected")
    }
}
