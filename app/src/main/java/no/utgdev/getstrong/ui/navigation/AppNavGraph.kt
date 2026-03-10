package no.utgdev.getstrong.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import androidx.annotation.DrawableRes
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import no.utgdev.getstrong.ui.activeWorkout.ActiveWorkoutScreen
import no.utgdev.getstrong.ui.activeWorkout.ActiveWorkoutViewModel
import no.utgdev.getstrong.ui.history.HistoryScreen
import no.utgdev.getstrong.ui.history.HistoryViewModel
import no.utgdev.getstrong.ui.history.ExerciseHistoryScreen
import no.utgdev.getstrong.ui.history.ExerciseHistoryViewModel
import no.utgdev.getstrong.ui.home.HomeScreen
import no.utgdev.getstrong.ui.planning.PlanningScreen
import no.utgdev.getstrong.ui.planning.PlanningViewModel
import no.utgdev.getstrong.ui.progress.ProgressScreen
import no.utgdev.getstrong.ui.planning.WorkoutEditorScreen
import no.utgdev.getstrong.ui.planning.WorkoutEditorViewModel
import no.utgdev.getstrong.ui.settings.SettingsScreen
import no.utgdev.getstrong.ui.settings.SettingsViewModel
import no.utgdev.getstrong.ui.summary.SummaryScreen
import no.utgdev.getstrong.ui.summary.SummaryViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph(navController: NavHostController) {
    val topLevelDestinations = listOf(
        TopLevelDestination(AppDestination.Home, "Home", android.R.drawable.ic_menu_view),
        TopLevelDestination(AppDestination.Programs, "Programs", android.R.drawable.ic_menu_agenda),
        TopLevelDestination(AppDestination.History, "History", android.R.drawable.ic_menu_recent_history),
        TopLevelDestination(AppDestination.Progress, "Progress", android.R.drawable.ic_menu_sort_by_size),
        TopLevelDestination(AppDestination.Settings, "Settings", android.R.drawable.ic_menu_preferences),
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isTopLevelRoute = topLevelDestinations.any { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.destination.route } == true
    }

    Scaffold(
        bottomBar = {
            if (isTopLevelRoute) {
                NavigationBar {
                    topLevelDestinations.forEach { topLevel ->
                        val selected = currentDestination?.hierarchy?.any { it.route == topLevel.destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(topLevel.destination.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(id = topLevel.iconRes),
                                    contentDescription = topLevel.label,
                                )
                            },
                            label = { Text(topLevel.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            modifier = Modifier.padding(innerPadding),
            navController = navController,
            startDestination = AppDestination.Home.route,
        ) {
        composable(route = AppDestination.Home.route) {
            HomeScreen(
                onStartWorkout = { navController.navigate(AppDestination.Programs.route) },
            )
        }

        composable(route = AppDestination.Settings.route) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settingsUiState by settingsViewModel.uiState.collectAsState()
            SettingsScreen(
                uiState = settingsUiState,
                onRestDurationChanged = settingsViewModel::updateRestDurationInput,
                onIncrementChanged = settingsViewModel::updateIncrementInput,
                onDeloadPercentChanged = settingsViewModel::updateDeloadInput,
                onProgressionModeChanged = settingsViewModel::updateProgressionMode,
                onSave = settingsViewModel::save,
            )
        }

        composable(route = AppDestination.History.route) {
            val historyViewModel: HistoryViewModel = hiltViewModel()
            val historyUiState by historyViewModel.uiState.collectAsState()
            HistoryScreen(
                uiState = historyUiState,
                onRetry = { historyViewModel.load() },
                onStartWorkoutFlow = { navController.navigate(AppDestination.Programs.route) },
                onOpenExerciseHistory = { exerciseId ->
                    navController.navigate(AppDestination.ExerciseHistory.route(exerciseId))
                },
            )
        }

        composable(route = AppDestination.Progress.route) {
            ProgressScreen()
        }

        composable(route = AppDestination.Programs.route) {
            val planningViewModel: PlanningViewModel = hiltViewModel()
            val planningUiState by planningViewModel.uiState.collectAsState()
            val coroutineScope = rememberCoroutineScope()
            LaunchedEffect(Unit) {
                planningViewModel.refresh()
            }
            PlanningScreen(
                uiState = planningUiState,
                onCreateWorkout = {
                    navController.navigate(AppDestination.WorkoutEditor.route(workoutId = null))
                },
                onRetryLoad = { planningViewModel.refresh() },
                onEditWorkout = { workoutId ->
                    navController.navigate(AppDestination.WorkoutEditor.route(workoutId = workoutId))
                },
                onDeleteWorkout = { workoutId ->
                    planningViewModel.deleteWorkout(workoutId)
                },
                onStartWorkout = { workoutId ->
                    coroutineScope.launch {
                        val sessionId = planningViewModel.startWorkoutSession(workoutId)
                        if (sessionId != null) {
                            navController.navigate(AppDestination.ActiveWorkout.route(sessionId))
                        }
                    }
                },
            )
        }

        composable(
            route = AppDestination.ExerciseHistory.route,
            arguments = listOf(navArgument(AppDestination.ExerciseHistory.EXERCISE_ID_ARG) { type = NavType.StringType }),
        ) {
            val exerciseHistoryViewModel: ExerciseHistoryViewModel = hiltViewModel()
            val exerciseHistoryUiState by exerciseHistoryViewModel.uiState.collectAsState()
            ExerciseHistoryScreen(
                uiState = exerciseHistoryUiState,
                onBack = { navController.popBackStack() },
                onRetry = { exerciseHistoryViewModel.load() },
            )
        }

        composable(
            route = AppDestination.WorkoutEditor.route,
            arguments = listOf(navArgument(AppDestination.WorkoutEditor.WORKOUT_ID_ARG) { type = NavType.StringType }),
        ) {
            val editorViewModel: WorkoutEditorViewModel = hiltViewModel()
            val editorUiState by editorViewModel.uiState.collectAsState()
            val coroutineScope = rememberCoroutineScope()
            WorkoutEditorScreen(
                uiState = editorUiState,
                onNameChanged = editorViewModel::setName,
                onAddExercise = editorViewModel::addExercise,
                onRemoveSlot = editorViewModel::removeSlot,
                onMoveSlotUp = editorViewModel::moveSlotUp,
                onMoveSlotDown = editorViewModel::moveSlotDown,
                onUpdateSlotTargets = editorViewModel::updateSlotTargets,
                onSave = {
                    coroutineScope.launch {
                        editorViewModel.save()
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() },
                onClearMessage = editorViewModel::clearMessage,
            )
        }

        composable(
            route = AppDestination.ActiveWorkout.route,
            arguments = listOf(navArgument(AppDestination.ActiveWorkout.SESSION_ID_ARG) { type = NavType.StringType }),
        ) {
            val activeWorkoutViewModel: ActiveWorkoutViewModel = hiltViewModel()
            val activeUiState by activeWorkoutViewModel.uiState.collectAsState()
            val coroutineScope = rememberCoroutineScope()
            ActiveWorkoutScreen(
                uiState = activeUiState,
                onToggleSet = activeWorkoutViewModel::onSetTapped,
                onSetReps = activeWorkoutViewModel::completeSet,
                onSetWeight = activeWorkoutViewModel::updateSetWeight,
                onClearSet = activeWorkoutViewModel::clearSet,
                onAddExtraSet = activeWorkoutViewModel::addExtraSet,
                onRemoveExtraSet = activeWorkoutViewModel::removeExtraSet,
                onFinishSession = {
                    coroutineScope.launch {
                        val finishedSessionId = activeWorkoutViewModel.finishSession()
                        navController.navigate(AppDestination.Summary.route(finishedSessionId.toString())) {
                            popUpTo(AppDestination.ActiveWorkout.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                onExit = {
                    navController.popBackStack()
                },
            )
        }

        composable(
            route = AppDestination.Summary.route,
            arguments = listOf(navArgument(AppDestination.Summary.SESSION_ID_ARG) { type = NavType.StringType }),
        ) {
            val summaryViewModel: SummaryViewModel = hiltViewModel()
            val summaryUiState by summaryViewModel.uiState.collectAsState()
            SummaryScreen(
                uiState = summaryUiState,
                onRetryLoad = { summaryViewModel.loadSummary() },
                onDone = {
                    navController.navigate(AppDestination.Home.route) {
                        popUpTo(AppDestination.Home.route) { inclusive = true }
                    }
                },
            )
        }
        }
    }
}

private data class TopLevelDestination(
    val destination: AppDestination,
    val label: String,
    @DrawableRes val iconRes: Int,
)
