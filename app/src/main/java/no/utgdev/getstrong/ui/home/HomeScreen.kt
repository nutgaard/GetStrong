package no.utgdev.getstrong.ui.home

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onOpenPlanning: () -> Unit,
    onStartWorkout: () -> Unit,
    onRunPersistenceDemo: () -> Unit,
    onLoadCatalog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(text = "GetStrong", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Plan and run your workouts")

        Button(onClick = onOpenPlanning) {
            Text("Workout Planning")
        }

        Button(onClick = onStartWorkout) {
            Text("Start Active Workout")
        }

        Button(
            onClick = onRunPersistenceDemo,
            enabled = !uiState.isRunningDemo,
        ) {
            Text(if (uiState.isRunningDemo) "Running..." else "Run Persistence Demo")
        }

        Text(text = uiState.demoResultMessage)
        Text(text = "Catalog count: ${uiState.catalogCount}")

        Button(
            onClick = onLoadCatalog,
            enabled = !uiState.isLoadingCatalog,
        ) {
            Text(if (uiState.isLoadingCatalog) "Loading..." else "Refresh Catalog")
        }

        uiState.catalogPreview.forEach { previewRow ->
            Text(text = previewRow, style = MaterialTheme.typography.bodySmall)
        }
    }
}
