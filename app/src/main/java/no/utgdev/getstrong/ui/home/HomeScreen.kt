package no.utgdev.getstrong.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onOpenPlanning: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartWorkout: () -> Unit,
    onRunPersistenceDemo: () -> Unit,
    onLoadCatalog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onStartWorkout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .semantics { contentDescription = "Start active workout from planning" },
                ) {
                    Text("Start Active Workout")
                }
                Button(
                    onClick = onOpenPlanning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .semantics { contentDescription = "Open workout planning" },
                ) {
                    Text("Workout Planning")
                }
                Button(
                    onClick = onOpenHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .semantics { contentDescription = "Open workout history" },
                ) {
                    Text("Workout History")
                }
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .semantics { contentDescription = "Open settings" },
                ) {
                    Text("Settings")
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = "GetStrong",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = "Plan and run your workouts")
            Button(
                onClick = onRunPersistenceDemo,
                enabled = !uiState.isRunningDemo,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = "Run persistence demo"
                        stateDescription = if (uiState.isRunningDemo) "Running" else "Idle"
                    },
            ) {
                Text(if (uiState.isRunningDemo) "Running..." else "Run Persistence Demo")
            }

            Text(
                text = uiState.demoResultMessage,
                modifier = Modifier.semantics { contentDescription = "Persistence demo result ${uiState.demoResultMessage}" },
            )
            Text(
                text = "Catalog count: ${uiState.catalogCount}",
                modifier = Modifier.semantics { contentDescription = "Catalog count ${uiState.catalogCount}" },
            )

            Button(
                onClick = onLoadCatalog,
                enabled = !uiState.isLoadingCatalog,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = "Refresh exercise catalog"
                        stateDescription = if (uiState.isLoadingCatalog) "Loading" else "Idle"
                    },
            ) {
                Text(if (uiState.isLoadingCatalog) "Loading..." else "Refresh Catalog")
            }

            uiState.catalogPreview.forEach { previewRow ->
                Text(text = previewRow, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.padding(bottom = 8.dp))
        }
    }
}
