package no.utgdev.getstrong.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import no.utgdev.getstrong.ui.activeWorkout.ActiveWorkoutScreen
import no.utgdev.getstrong.ui.activeWorkout.ActiveWorkoutViewModel
import no.utgdev.getstrong.ui.history.HistoryScreen
import no.utgdev.getstrong.ui.history.HistoryViewModel
import no.utgdev.getstrong.ui.home.HomeScreen
import no.utgdev.getstrong.ui.home.HomeViewModel
import no.utgdev.getstrong.ui.planning.PlanningScreen
import no.utgdev.getstrong.ui.planning.PlanningViewModel
import no.utgdev.getstrong.ui.planning.WorkoutEditorScreen
import no.utgdev.getstrong.ui.planning.WorkoutEditorViewModel
import no.utgdev.getstrong.ui.settings.SettingsScreen
import no.utgdev.getstrong.ui.settings.SettingsViewModel
import no.utgdev.getstrong.ui.summary.SummaryScreen
import no.utgdev.getstrong.ui.summary.SummaryViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Home.route,
    ) {
        composable(route = AppDestination.Home.route) {
            val homeViewModel: HomeViewModel = hiltViewModel()
            val uiState by homeViewModel.uiState.collectAsState()
            HomeScreen(
                uiState = uiState,
                onOpenPlanning = { navController.navigate(AppDestination.Planning.route) },
                onOpenHistory = { navController.navigate(AppDestination.History.route) },
                onOpenSettings = { navController.navigate(AppDestination.Settings.route) },
                onStartWorkout = { navController.navigate(AppDestination.Planning.route) },
                onRunPersistenceDemo = { homeViewModel.runPersistenceDemo() },
                onLoadCatalog = { homeViewModel.loadCatalog() },
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
                onBack = { navController.popBackStack() },
            )
        }

        composable(route = AppDestination.History.route) {
            val historyViewModel: HistoryViewModel = hiltViewModel()
            val historyUiState by historyViewModel.uiState.collectAsState()
            HistoryScreen(
                uiState = historyUiState,
                onBack = { navController.popBackStack() },
                onRetry = { historyViewModel.load() },
                onStartWorkoutFlow = { navController.navigate(AppDestination.Planning.route) },
            )
        }

        composable(route = AppDestination.Planning.route) {
            val planningViewModel: PlanningViewModel = hiltViewModel()
            val planningUiState by planningViewModel.uiState.collectAsState()
            val coroutineScope = rememberCoroutineScope()
            LaunchedEffect(Unit) {
                planningViewModel.refresh()
            }
            PlanningScreen(
                uiState = planningUiState,
                onBack = { navController.popBackStack() },
                onCreateWorkout = {
                    navController.navigate(AppDestination.PlanningEditor.route(workoutId = null))
                },
                onRetryLoad = { planningViewModel.refresh() },
                onEditWorkout = { workoutId ->
                    navController.navigate(AppDestination.PlanningEditor.route(workoutId = workoutId))
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
            route = AppDestination.PlanningEditor.route,
            arguments = listOf(navArgument(AppDestination.PlanningEditor.WORKOUT_ID_ARG) { type = NavType.StringType }),
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
                onSave = {
                    coroutineScope.launch {
                        editorViewModel.save()
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() },
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
                onCompleteSet = { setId, repsAchieved ->
                    activeWorkoutViewModel.completeSet(setId, repsAchieved)
                },
                onFocusSet = { setId ->
                    activeWorkoutViewModel.focusSet(setId)
                },
                onFinishSession = {
                    coroutineScope.launch {
                        val finishedSessionId = activeWorkoutViewModel.finishSession()
                        navController.navigate(AppDestination.Summary.route(finishedSessionId.toString()))
                    }
                },
                onExit = {
                    navController.navigate(AppDestination.Home.route) {
                        popUpTo(AppDestination.Home.route) { inclusive = true }
                    }
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
