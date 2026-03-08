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
import no.utgdev.getstrong.ui.home.HomeScreen
import no.utgdev.getstrong.ui.home.HomeViewModel
import no.utgdev.getstrong.ui.planning.PlanningScreen
import no.utgdev.getstrong.ui.planning.PlanningViewModel
import no.utgdev.getstrong.ui.planning.WorkoutEditorScreen
import no.utgdev.getstrong.ui.planning.WorkoutEditorViewModel
import no.utgdev.getstrong.ui.summary.SummaryScreen
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
                onStartWorkout = { navController.navigate(AppDestination.ActiveWorkout.route("quick-start")) },
                onRunPersistenceDemo = { homeViewModel.runPersistenceDemo() },
                onLoadCatalog = { homeViewModel.loadCatalog() },
            )
        }

        composable(route = AppDestination.Planning.route) {
            val planningViewModel: PlanningViewModel = hiltViewModel()
            val planningUiState by planningViewModel.uiState.collectAsState()
            LaunchedEffect(Unit) {
                planningViewModel.refresh()
            }
            PlanningScreen(
                uiState = planningUiState,
                onBack = { navController.popBackStack() },
                onCreateWorkout = {
                    navController.navigate(AppDestination.PlanningEditor.route(workoutId = null))
                },
                onEditWorkout = { workoutId ->
                    navController.navigate(AppDestination.PlanningEditor.route(workoutId = workoutId))
                },
                onDeleteWorkout = { workoutId ->
                    planningViewModel.deleteWorkout(workoutId)
                },
                onStartWorkout = { workoutId ->
                    navController.navigate(AppDestination.ActiveWorkout.route(workoutId.toString()))
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
            arguments = listOf(navArgument(AppDestination.ActiveWorkout.WORKOUT_ID_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val workoutId =
                backStackEntry.arguments?.getString(AppDestination.ActiveWorkout.WORKOUT_ID_ARG).orEmpty()

            ActiveWorkoutScreen(
                workoutId = workoutId,
                onComplete = { sessionId ->
                    navController.navigate(AppDestination.Summary.route(sessionId))
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
        ) { backStackEntry ->
            val sessionId =
                backStackEntry.arguments?.getString(AppDestination.Summary.SESSION_ID_ARG).orEmpty()

            SummaryScreen(
                sessionId = sessionId,
                onDone = {
                    navController.navigate(AppDestination.Home.route) {
                        popUpTo(AppDestination.Home.route) { inclusive = true }
                    }
                },
            )
        }
    }
}
