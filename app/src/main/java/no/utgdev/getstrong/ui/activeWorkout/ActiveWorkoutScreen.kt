package no.utgdev.getstrong.ui.activeWorkout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.SessionSetType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    uiState: ActiveWorkoutUiState,
    onToggleSet: (Long) -> Unit,
    onSetReps: (Long, Int) -> Unit,
    onSetWeight: (Long, Double) -> Unit,
    onClearSet: (Long) -> Unit,
    onAddExtraSet: (Long) -> Unit,
    onRemoveExtraSet: (Long) -> Unit,
    onFinishSession: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KeepScreenOnEffect(enabled = uiState.isSessionActive)
    val currentSet = uiState.currentSet
    var selectedSection by rememberSaveable(currentSet?.setType) {
        mutableStateOf(sectionForSet(currentSet))
    }
    var actionSetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editRepsSetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editWeightSetId by rememberSaveable { mutableStateOf<Long?>(null) }

    val actionSet = uiState.plannedSets.firstOrNull { it.id == actionSetId }
    val repsSet = uiState.plannedSets.firstOrNull { it.id == editRepsSetId }
    val weightSet = uiState.plannedSets.firstOrNull { it.id == editWeightSetId }
    val workoutGroups = remember(uiState.plannedSets, uiState.exerciseNamesById) {
        buildExerciseGroups(
            plannedSets = uiState.plannedSets,
            exerciseNamesById = uiState.exerciseNamesById,
            setType = SessionSetType.WORK,
        )
    }
    val warmupGroups = remember(uiState.plannedSets, uiState.exerciseNamesById) {
        buildExerciseGroups(
            plannedSets = uiState.plannedSets,
            exerciseNamesById = uiState.exerciseNamesById,
            setType = SessionSetType.WARMUP,
        )
    }
    val visibleGroups = if (selectedSection == ActiveWorkoutSection.WORKOUT) workoutGroups else warmupGroups
    val completedCount = uiState.plannedSets.count { it.isCompleted }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Active Workout") },
                navigationIcon = {
                    TextButton(onClick = onExit) {
                        Text("Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = onFinishSession,
                        enabled = uiState.isCompleted,
                    ) {
                        Text("Finish")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                    bottom = 120.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SessionStatsCard(
                        elapsedSeconds = uiState.elapsedSeconds,
                        completedCount = completedCount,
                        totalCount = uiState.plannedSets.size,
                    )
                }

                item {
                    CurrentSetCard(
                        currentSet = currentSet,
                        exerciseName = currentSet?.let { uiState.exerciseNamesById[it.exerciseId] ?: "Exercise ${it.exerciseId}" },
                        isHighlighted = currentSet?.id == uiState.highlightedSetId,
                        onToggleSet = { setId ->
                            onToggleSet(setId)
                            actionSetId = null
                        },
                        onOpenActions = { setId -> actionSetId = setId },
                    )
                }

                item {
                    PrimaryTabRow(selectedTabIndex = selectedSection.ordinal) {
                        ActiveWorkoutSection.entries.forEach { section ->
                            Tab(
                                selected = selectedSection == section,
                                onClick = { selectedSection = section },
                                text = { Text(section.title) },
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Tap a set to complete or decrement. Long-press for reps, weight, reset, and extra-set actions.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                if (visibleGroups.isEmpty()) {
                    item {
                        EmptySectionCard(selectedSection = selectedSection)
                    }
                } else {
                    items(visibleGroups, key = { it.workoutSlotId }) { group ->
                        ExerciseSetGroupCard(
                            group = group,
                            currentSetId = currentSet?.id,
                            highlightedSetId = uiState.highlightedSetId,
                            onToggleSet = { setId ->
                                onToggleSet(setId)
                                actionSetId = null
                            },
                            onOpenActions = { setId -> actionSetId = setId },
                            onAddExtraSet = { onAddExtraSet(group.sets.last().id) },
                        )
                    }
                }
            }

            if (uiState.isRestTimerActive || uiState.isRestOver) {
                RestTimerOverlay(
                    remainingSeconds = uiState.restRemainingSeconds,
                    isRestOver = uiState.isRestOver,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                )
            }
        }
    }

    if (actionSet != null) {
        SetActionsDialog(
            set = actionSet,
            exerciseName = uiState.exerciseNamesById[actionSet.exerciseId] ?: "Exercise ${actionSet.exerciseId}",
            onDismiss = { actionSetId = null },
            onSetReps = {
                actionSetId = null
                editRepsSetId = actionSet.id
            },
            onSetWeight = {
                actionSetId = null
                editWeightSetId = actionSet.id
            },
            onClearSet = {
                actionSetId = null
                onClearSet(actionSet.id)
            },
            onRemoveExtraSet = if (actionSet.isExtra) {
                {
                    actionSetId = null
                    onRemoveExtraSet(actionSet.id)
                }
            } else {
                null
            },
        )
    }

    if (repsSet != null) {
        EditSetRepsDialog(
            set = repsSet,
            exerciseName = uiState.exerciseNamesById[repsSet.exerciseId] ?: "Exercise ${repsSet.exerciseId}",
            onDismiss = { editRepsSetId = null },
            onSave = { reps ->
                editRepsSetId = null
                onSetReps(repsSet.id, reps)
            },
        )
    }

    if (weightSet != null) {
        EditSetWeightDialog(
            set = weightSet,
            exerciseName = uiState.exerciseNamesById[weightSet.exerciseId] ?: "Exercise ${weightSet.exerciseId}",
            onDismiss = { editWeightSetId = null },
            onSave = { weight ->
                editWeightSetId = null
                onSetWeight(weightSet.id, weight)
            },
        )
    }
}

@Composable
private fun SessionStatsCard(
    elapsedSeconds: Long,
    completedCount: Int,
    totalCount: Int,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Elapsed", style = MaterialTheme.typography.labelMedium)
                Text(formatElapsed(elapsedSeconds), style = MaterialTheme.typography.titleLarge)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Completed", style = MaterialTheme.typography.labelMedium)
                Text("$completedCount / $totalCount", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun CurrentSetCard(
    currentSet: SessionPlannedSet?,
    exerciseName: String?,
    isHighlighted: Boolean,
    onToggleSet: (Long) -> Unit,
    onOpenActions: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (currentSet == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Current Set", style = MaterialTheme.typography.labelLarge)
                Text("All planned sets are complete.", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SetCircle(
                    set = currentSet,
                    isCurrent = true,
                    isHighlighted = isHighlighted,
                    onClick = { onToggleSet(currentSet.id) },
                    onLongClick = { onOpenActions(currentSet.id) },
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(currentSetTitle(currentSet), style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = exerciseName ?: "Exercise ${currentSet.exerciseId}",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = buildSetDetailText(currentSet),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySectionCard(selectedSection: ActiveWorkoutSection) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "No ${selectedSection.title.lowercase()} sets in this session section.",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun ExerciseSetGroupCard(
    group: ExerciseSetGroup,
    currentSetId: Long?,
    highlightedSetId: Long?,
    onToggleSet: (Long) -> Unit,
    onOpenActions: (Long) -> Unit,
    onAddExtraSet: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(group.exerciseName, style = MaterialTheme.typography.titleMedium)
                    Text(group.prescriptionSummary, style = MaterialTheme.typography.bodySmall)
                }
                if (group.sets.any { it.id == currentSetId }) {
                    Text(
                        text = "Current",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                group.sets.forEach { set ->
                    SetCircle(
                        set = set,
                        isCurrent = currentSetId == set.id,
                        isHighlighted = highlightedSetId == set.id,
                        onClick = { onToggleSet(set.id) },
                        onLongClick = { onOpenActions(set.id) },
                    )
                }
                AddSetCircle(onClick = onAddExtraSet)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SetCircle(
    set: SessionPlannedSet,
    isCurrent: Boolean,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val achievedReps = set.completedReps ?: 0
    val isTouched = achievedReps > 0
    val fillColor = when {
        isCurrent -> MaterialTheme.colorScheme.primary
        isTouched -> MaterialTheme.colorScheme.primaryContainer
        set.setType == SessionSetType.WARMUP -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val borderColor = when {
        isHighlighted || isCurrent -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    val labelColor = when {
        isCurrent -> MaterialTheme.colorScheme.onPrimary
        isTouched -> MaterialTheme.colorScheme.onPrimaryContainer
        set.setType == SessionSetType.WARMUP -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val displayedReps = if (achievedReps > 0) achievedReps else set.targetReps

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.semantics {
            contentDescription = buildString {
                append("${if (set.setType == SessionSetType.WARMUP) "Warmup" else "Workout"} set ${set.setOrder + 1}")
                append(". Target ${set.targetReps} reps")
                set.targetWeightKg?.let { append(" at ${formatWeight(it)}") }
                if (achievedReps > 0) {
                    append(". Achieved $achievedReps reps")
                }
            }
            stateDescription = when {
                achievedReps <= 0 -> "Incomplete"
                achievedReps >= set.targetReps -> "Completed"
                else -> "Partial"
            }
        },
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(fillColor)
                .border(width = 2.dp, color = borderColor, shape = CircleShape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = displayedReps.toString(),
                color = labelColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
            )
        }
        Text(
            text = if (set.isExtra) "Extra" else "#${set.setOrder + 1}",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AddSetCircle(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
            )
        }
        Text("Add", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun RestTimerOverlay(
    remainingSeconds: Int,
    isRestOver: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (isRestOver) "Rest complete" else "Rest timer",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = if (isRestOver) "Start your next work set." else "Stay on this screen while the countdown runs.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = if (isRestOver) "Go" else "${remainingSeconds}s",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SetActionsDialog(
    set: SessionPlannedSet,
    exerciseName: String,
    onDismiss: () -> Unit,
    onSetReps: () -> Unit,
    onSetWeight: () -> Unit,
    onClearSet: () -> Unit,
    onRemoveExtraSet: (() -> Unit)?,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exerciseName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = buildSetDetailText(set),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                ActionTextButton(text = "Set reps", onClick = onSetReps)
                ActionTextButton(text = "Set weight", onClick = onSetWeight)
                ActionTextButton(text = "Clear / reset set", onClick = onClearSet)
                if (onRemoveExtraSet != null) {
                    ActionTextButton(text = "Remove extra set", onClick = onRemoveExtraSet)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@Composable
private fun ActionTextButton(
    text: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
private fun EditSetRepsDialog(
    set: SessionPlannedSet,
    exerciseName: String,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var repsInput by rememberSaveable(set.id) {
        mutableStateOf((set.completedReps ?: set.targetReps).toString())
    }
    val parsed = repsInput.toIntOrNull()
    val canSave = parsed != null && parsed >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set reps for $exerciseName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(buildSetDetailText(set), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = repsInput,
                    onValueChange = { repsInput = it },
                    label = { Text("Achieved reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(parsed ?: 0) },
                enabled = canSave,
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun EditSetWeightDialog(
    set: SessionPlannedSet,
    exerciseName: String,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
) {
    var weightInput by rememberSaveable(set.id) {
        mutableStateOf(set.targetWeightKg?.toString().orEmpty())
    }
    val parsed = weightInput.toDoubleOrNull()
    val canSave = parsed != null && parsed >= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set weight for $exerciseName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(buildSetDetailText(set), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(parsed ?: 0.0) },
                enabled = canSave,
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private data class ExerciseSetGroup(
    val workoutSlotId: Long,
    val exerciseName: String,
    val prescriptionSummary: String,
    val sets: List<SessionPlannedSet>,
)

private enum class ActiveWorkoutSection(val title: String) {
    WORKOUT("Workout"),
    WARMUP("Warmup"),
}

private fun buildExerciseGroups(
    plannedSets: List<SessionPlannedSet>,
    exerciseNamesById: Map<Long, String>,
    setType: String,
): List<ExerciseSetGroup> {
    val grouped = linkedMapOf<Long, MutableList<SessionPlannedSet>>()
    plannedSets
        .filter { it.setType == setType }
        .sortedBy { it.setOrder }
        .forEach { plannedSet ->
            grouped.getOrPut(plannedSet.workoutSlotId) { mutableListOf() }.add(plannedSet)
        }

    return grouped.map { (workoutSlotId, sets) ->
        val sample = sets.first()
        ExerciseSetGroup(
            workoutSlotId = workoutSlotId,
            exerciseName = exerciseNamesById[sample.exerciseId] ?: "Exercise ${sample.exerciseId}",
            prescriptionSummary = buildPrescriptionSummary(sets),
            sets = sets,
        )
    }
}

private fun buildPrescriptionSummary(sets: List<SessionPlannedSet>): String {
    val sample = sets.firstOrNull() ?: return ""
    val repsText = if (sets.size == 1) {
        "${sample.targetReps} reps"
    } else {
        "${sets.size} sets x ${sample.targetReps} reps"
    }
    val weightText = sample.targetWeightKg?.let { " @ ${formatWeight(it)}" }.orEmpty()
    return repsText + weightText
}

private fun buildSetDetailText(set: SessionPlannedSet): String {
    val achieved = set.completedReps?.let { " | achieved $it" }.orEmpty()
    val extra = if (set.isExtra) " | extra" else ""
    return "Target ${set.targetReps} reps${set.targetWeightKg?.let { " @ ${formatWeight(it)}" } ?: ""}$achieved$extra"
}

private fun currentSetTitle(set: SessionPlannedSet): String =
    if (set.setType == SessionSetType.WARMUP) "Current Warmup Set" else "Current Workout Set"

private fun sectionForSet(set: SessionPlannedSet?): ActiveWorkoutSection =
    if (set?.setType == SessionSetType.WARMUP) ActiveWorkoutSection.WARMUP else ActiveWorkoutSection.WORKOUT

private fun formatWeight(weightKg: Double): String =
    if (weightKg % 1.0 == 0.0) {
        "${weightKg.toInt()} kg"
    } else {
        "$weightKg kg"
    }

private fun formatElapsed(seconds: Long): String {
    val total = seconds.coerceAtLeast(0L)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val secs = total % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, secs)
    } else {
        "%02d:%02d".format(minutes, secs)
    }
}

@Composable
private fun KeepScreenOnEffect(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        val previous = view.keepScreenOn
        if (enabled) {
            view.keepScreenOn = true
        }
        onDispose {
            view.keepScreenOn = previous
        }
    }
}
