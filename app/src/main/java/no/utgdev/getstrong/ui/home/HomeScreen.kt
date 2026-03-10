package no.utgdev.getstrong.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import no.utgdev.getstrong.ui.common.InlineStateCard

@Composable
fun HomeScreen(
    onStartWorkout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "GetStrong",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = "Home",
                style = MaterialTheme.typography.titleMedium,
            )
            InlineStateCard(
                title = "No upcoming workout is queued here yet.",
                body = "Open Programs to create a workout or start one from your saved workout list.",
                actionLabel = "Open Programs",
                onAction = onStartWorkout,
            )
        }
    }
}
