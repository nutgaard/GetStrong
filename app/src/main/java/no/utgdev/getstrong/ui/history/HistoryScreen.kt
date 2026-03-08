package no.utgdev.getstrong.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Workout History", style = MaterialTheme.typography.headlineMedium)
        uiState.items.forEach { item ->
            Text(
                text = "${item.workoutName} | session=${item.sessionId} | vol=${"%.1f".format(item.totalVolumeKg)}kg | time=${item.totalDurationSeconds}s",
            )
        }
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}
