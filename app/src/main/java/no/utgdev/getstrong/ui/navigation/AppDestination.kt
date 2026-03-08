package no.utgdev.getstrong.ui.navigation

sealed class AppDestination(val route: String) {
    data object Home : AppDestination("home")
    data object Planning : AppDestination("planning")
    data object PlanningEditor : AppDestination("planning/editor/{workoutId}") {
        const val WORKOUT_ID_ARG = "workoutId"
        const val NEW_WORKOUT_TOKEN = "new"

        fun route(workoutId: Long?): String {
            val encodedId = workoutId?.toString() ?: NEW_WORKOUT_TOKEN
            return "planning/editor/$encodedId"
        }
    }

    data object ActiveWorkout : AppDestination("activeWorkout/{workoutId}") {
        const val WORKOUT_ID_ARG = "workoutId"

        fun route(workoutId: String): String = "activeWorkout/$workoutId"
    }

    data object Summary : AppDestination("summary/{sessionId}") {
        const val SESSION_ID_ARG = "sessionId"

        fun route(sessionId: String): String = "summary/$sessionId"
    }
}
