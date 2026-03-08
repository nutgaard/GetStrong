package no.utgdev.getstrong.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import no.utgdev.getstrong.domain.model.SessionSetType

@Composable
fun SummaryScreen(
    uiState: SummaryUiState,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(text = "Workout Summary", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Session ID: ${uiState.sessionId}")
        Text(text = "Total time: ${uiState.totalDurationSeconds}s")
        Text(text = "Total volume: ${"%.1f".format(uiState.totalVolumeKg)} kg")
        Text(text = "Volume rule: ${uiState.volumeRule}")

        Column(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth()
                .padding(top = 8.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val scroll = rememberScrollState()
            Column(
                modifier = Modifier.verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                uiState.sets.forEach { row ->
                    val tint = if (row.setType == SessionSetType.WARMUP) Color(0xFFE0F2FE) else Color(0xFFE8F5E9)
                    Text(
                        text = formatSummaryRow(row),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(tint)
                            .padding(8.dp),
                    )
                }
            }
        }

        Button(onClick = onDone) {
            Text("Return Home")
        }
    }
}

private fun formatSummaryRow(row: SummarySetRowUi): String {
    val reps = row.achievedReps?.toString() ?: "-"
    val load = row.loadKg?.let { "${"%.1f".format(it)}kg" } ?: "-"
    return "#${row.setOrder + 1} ${row.setType} ex=${row.exerciseId} target=${row.targetReps} reps=$reps load=$load"
}
