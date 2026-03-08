package no.utgdev.getstrong.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import no.utgdev.getstrong.ui.activeWorkout.ActiveWorkoutScreen
import no.utgdev.getstrong.ui.home.HomeScreen
import no.utgdev.getstrong.ui.planning.PlanningScreen
import no.utgdev.getstrong.ui.summary.SummaryScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Home.route,
    ) {
        composable(route = AppDestination.Home.route) {
            HomeScreen(
                onOpenPlanning = { navController.navigate(AppDestination.Planning.route) },
                onStartWorkout = { navController.navigate(AppDestination.ActiveWorkout.route("quick-start")) },
            )
        }

        composable(route = AppDestination.Planning.route) {
            PlanningScreen(
                onBack = { navController.popBackStack() },
                onStartWorkout = { workoutId ->
                    navController.navigate(AppDestination.ActiveWorkout.route(workoutId))
                },
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
