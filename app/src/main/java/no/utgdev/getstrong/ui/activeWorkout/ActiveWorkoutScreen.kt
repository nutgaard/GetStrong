package no.utgdev.getstrong.ui.activeWorkout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
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
    BackHandler(onBack = onExit)
    KeepScreenOnEffect(enabled = uiState.isSessionActive)
    val currentSet = uiState.currentSet
    var selectedSection by rememberSaveable(currentSet?.setType) {
        mutableStateOf(sectionForSet(currentSet))
    }
    var actionSetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editRepsSetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editWeightSetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var restoreFocusSetId by rememberSaveable { mutableStateOf<Long?>(null) }
    val setFocusRequesters = remember { mutableStateMapOf<Long, FocusRequester>() }

    val actionSet = uiState.plannedSets.firstOrNull { it.id == actionSetId }
    val repsSet = uiState.plannedSets.firstOrNull { it.id == editRepsSetId }
    val weightSet = uiState.plannedSets.firstOrNull { it.id == editWeightSetId }
    val focusRequesterForSet: (Long) -> FocusRequester = { setId ->
        setFocusRequesters.getOrPut(setId) { FocusRequester() }
    }
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
    val bottomContentPadding = 148.dp

    LaunchedEffect(actionSetId, editRepsSetId, editWeightSetId, restoreFocusSetId) {
        if (actionSetId == null && editRepsSetId == null && editWeightSetId == null) {
            val targetSetId = restoreFocusSetId ?: return@LaunchedEffect
            setFocusRequesters[targetSetId]?.requestFocus()
            restoreFocusSetId = null
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Workout") },
                navigationIcon = {
                    TextButton(
                        onClick = onExit,
                        modifier = Modifier.semantics {
                            contentDescription = "Exit active workout"
                        },
                    ) {
                        Text("Back")
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
                modifier = Modifier
                    .fillMaxSize()
                    .semantics {
                        isTraversalGroup = true
                        traversalIndex = 2f
                    },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                    bottom = bottomContentPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SessionOverviewStrip(
                        elapsedSeconds = uiState.elapsedSeconds,
                        completedCount = completedCount,
                        totalCount = uiState.plannedSets.size,
                        selectedSection = selectedSection,
                        onSectionSelected = { selectedSection = it },
                        modifier = Modifier.semantics {
                            isTraversalGroup = true
                            traversalIndex = 0f
                        },
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
                            onOpenActions = { setId ->
                                restoreFocusSetId = setId
                                actionSetId = setId
                            },
                            onAddExtraSet = { onAddExtraSet(group.sets.last().id) },
                            focusRequesterForSet = focusRequesterForSet,
                        )
                    }
                }

                if (uiState.isCompleted) {
                    item {
                        CompletedSessionCard(onFinishSession = onFinishSession)
                    }
                }
            }

            if (uiState.isRestTimerActive || uiState.isRestOver) {
                RestTimerOverlay(
                    remainingSeconds = uiState.restRemainingSeconds,
                    isRestOver = uiState.isRestOver,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .semantics {
                            isTraversalGroup = true
                            traversalIndex = 1f
                        },
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
private fun SessionOverviewStrip(
    elapsedSeconds: Long,
    completedCount: Int,
    totalCount: Int,
    selectedSection: ActiveWorkoutSection,
    onSectionSelected: (ActiveWorkoutSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SupportPill(label = "Time", value = formatElapsed(elapsedSeconds))
            SupportPill(label = "Done", value = "$completedCount/$totalCount")
        }

        SectionSelector(
            selectedSection = selectedSection,
            onSectionSelected = onSectionSelected,
        )
    }
}

@Composable
private fun CompletedSessionCard(
    onFinishSession: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Workout Complete",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = "All planned sets are complete. Finish when you're ready to review the summary.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onFinishSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Finish workout and open summary"
                    },
            ) {
                Text("Finish Workout")
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
    focusRequesterForSet: (Long) -> FocusRequester,
) {
    val containsCurrentSet = group.sets.any { it.id == currentSetId }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (containsCurrentSet) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(group.exerciseName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = group.prescriptionSummary,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (containsCurrentSet) {
                    SupportPill(label = "Current", value = "Now")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                group.sets.forEachIndexed { index, set ->
                    SetCircle(
                        set = set,
                        exerciseName = group.exerciseName,
                        setIndex = index + 1,
                        setCount = group.sets.size,
                        isCurrent = currentSetId == set.id,
                        isHighlighted = highlightedSetId == set.id,
                        diameter = 62.dp,
                        onClick = { onToggleSet(set.id) },
                        onLongClick = { onOpenActions(set.id) },
                        focusRequester = focusRequesterForSet(set.id),
                    )
                }
                AddSetCircle(
                    exerciseName = group.exerciseName,
                    setType = group.sets.firstOrNull()?.setType ?: SessionSetType.WORK,
                    onClick = onAddExtraSet,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SetCircle(
    set: SessionPlannedSet,
    exerciseName: String,
    setIndex: Int,
    setCount: Int,
    isCurrent: Boolean,
    isHighlighted: Boolean,
    diameter: Dp = 58.dp,
    showCaption: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    focusRequester: FocusRequester,
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
    val circleTextSize = if (diameter >= 72.dp) 28.sp else 20.sp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(diameter)
                .clip(CircleShape)
                .background(fillColor)
                .border(width = if (isCurrent) 3.dp else 2.dp, color = borderColor, shape = CircleShape)
                .focusRequester(focusRequester)
                .focusable()
                .combinedClickable(
                    onClickLabel = buildSetToggleActionLabel(
                        exerciseName = exerciseName,
                        set = set,
                        setIndex = setIndex,
                    ),
                    onLongClickLabel = "Open actions for ${buildSetShortDescription(exerciseName, set, setIndex, setCount)}",
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .semantics {
                    contentDescription = buildSetAccessibilityDescription(
                        exerciseName = exerciseName,
                        set = set,
                        setIndex = setIndex,
                        setCount = setCount,
                        isCurrent = isCurrent,
                    )
                    stateDescription = buildSetAccessibilityStateDescription(
                        set = set,
                        isCurrent = isCurrent,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = displayedReps.toString(),
                color = labelColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = circleTextSize,
            )
        }
        if (showCaption) {
            Text(
                text = if (set.isExtra) "Extra" else "#$setIndex",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}

@Composable
private fun AddSetCircle(
    exerciseName: String,
    setType: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .combinedClickable(
                    onClickLabel = "Add extra ${sectionLabelForSetType(setType).lowercase()} set for $exerciseName",
                    onClick = onClick,
                    onLongClick = onClick,
                )
                .semantics {
                    contentDescription = "Add extra ${sectionLabelForSetType(setType).lowercase()} set for $exerciseName"
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
            )
        }
        Text(
            text = "Add",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

@Composable
private fun RestTimerOverlay(
    remainingSeconds: Int,
    isRestOver: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 360.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = if (isRestOver) {
                        "Rest complete. Start the next set."
                    } else {
                        "Rest timer. $remainingSeconds seconds remaining."
                    }
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (isRestOver) "Rest complete" else "Rest",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = if (isRestOver) "Start the next set." else "Countdown running in-session.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = if (isRestOver) "Go" else "${remainingSeconds}s",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SectionSelector(
    selectedSection: ActiveWorkoutSection,
    onSectionSelected: (ActiveWorkoutSection) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActiveWorkoutSection.entries.forEach { section ->
            FilterChip(
                selected = selectedSection == section,
                onClick = { onSectionSelected(section) },
                modifier = Modifier.semantics {
                    contentDescription = "Show ${section.title.lowercase()} sets"
                    stateDescription = if (selectedSection == section) "Selected" else "Not selected"
                },
                label = { Text(section.title) },
            )
        }
    }
}

@Composable
private fun SupportPill(
    label: String,
    value: String,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = "$label $value"
            },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
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
        title = { Text("Set actions for $exerciseName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = buildSetDetailText(set),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                ActionTextButton(
                    text = "Set reps",
                    contentDescription = "Set reps for ${buildDialogSetDescription(exerciseName, set)}",
                    onClick = onSetReps,
                )
                ActionTextButton(
                    text = "Set weight",
                    contentDescription = "Set weight for ${buildDialogSetDescription(exerciseName, set)}",
                    onClick = onSetWeight,
                )
                ActionTextButton(
                    text = "Clear / reset set",
                    contentDescription = "Clear or reset ${buildDialogSetDescription(exerciseName, set)}",
                    onClick = onClearSet,
                )
                if (onRemoveExtraSet != null) {
                    ActionTextButton(
                        text = "Remove extra set",
                        contentDescription = "Remove extra set for $exerciseName",
                        onClick = onRemoveExtraSet,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription = "Close set actions for $exerciseName"
                },
            ) {
                Text("Close")
            }
        },
    )
}

@Composable
private fun ActionTextButton(
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                this.contentDescription = contentDescription
            },
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
                modifier = Modifier.semantics {
                    contentDescription = "Save reps for $exerciseName"
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription = "Cancel editing reps for $exerciseName"
                },
            ) {
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
                modifier = Modifier.semantics {
                    contentDescription = "Save weight for $exerciseName"
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription = "Cancel editing weight for $exerciseName"
                },
            ) {
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

internal fun buildSetAccessibilityDescription(
    exerciseName: String,
    set: SessionPlannedSet,
    setIndex: Int,
    setCount: Int,
    isCurrent: Boolean,
): String =
    buildString {
        append(buildSetShortDescription(exerciseName, set, setIndex, setCount))
        if (isCurrent) {
            append(". Current set.")
        } else {
            append(".")
        }
        append(" Target ${set.targetReps} reps")
        set.targetWeightKg?.let { append(" at ${formatWeight(it)}") }
        append(". Achieved ${set.completedReps ?: 0} reps.")
    }

internal fun buildSetAccessibilityStateDescription(
    set: SessionPlannedSet,
    isCurrent: Boolean,
): String {
    val completion = when {
        (set.completedReps ?: 0) <= 0 -> "Incomplete"
        (set.completedReps ?: 0) >= set.targetReps -> "Completed"
        else -> "Partial"
    }
    return if (isCurrent) "Current, $completion" else completion
}

internal fun buildSetToggleActionLabel(
    exerciseName: String,
    set: SessionPlannedSet,
    setIndex: Int,
): String =
    when {
        (set.completedReps ?: 0) <= 0 ->
            "Complete ${sectionLabelForSetType(set.setType).lowercase()} set $setIndex for $exerciseName"
        else ->
            "Reduce achieved reps for ${sectionLabelForSetType(set.setType).lowercase()} set $setIndex for $exerciseName"
    }

internal fun buildSetShortDescription(
    exerciseName: String,
    set: SessionPlannedSet,
    setIndex: Int,
    setCount: Int,
): String =
    buildString {
        append(exerciseName)
        append(". ${sectionLabelForSetType(set.setType)} ")
        if (set.isExtra) {
            append("extra set")
        } else {
            append("set $setIndex of $setCount")
        }
    }

private fun buildDialogSetDescription(
    exerciseName: String,
    set: SessionPlannedSet,
): String =
    buildString {
        append(exerciseName)
        append(". ${sectionLabelForSetType(set.setType)} ")
        if (set.isExtra) {
            append("extra set")
        } else {
            append("set ${set.setOrder + 1}")
        }
    }

private fun sectionForSet(set: SessionPlannedSet?): ActiveWorkoutSection =
    if (set?.setType == SessionSetType.WARMUP) ActiveWorkoutSection.WARMUP else ActiveWorkoutSection.WORKOUT

private fun formatWeight(weightKg: Double): String =
    if (weightKg % 1.0 == 0.0) {
        "${weightKg.toInt()} kg"
    } else {
        "$weightKg kg"
    }

private fun sectionLabelForSetType(setType: String): String =
    if (setType == SessionSetType.WARMUP) "Warmup" else "Workout"

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
