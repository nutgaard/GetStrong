package no.utgdev.getstrong.ui.navigation

sealed class AppDestination(val route: String) {
    data object Home : AppDestination("home")
    data object Programs : AppDestination("programs")
    data object History : AppDestination("history")
    data object Progress : AppDestination("progress")
    data object Settings : AppDestination("settings")
    data object WorkoutEditor : AppDestination("workoutEditor/{workoutId}") {
        const val WORKOUT_ID_ARG = "workoutId"
        const val NEW_WORKOUT_TOKEN = "new"

        fun route(workoutId: Long?): String {
            val encodedId = workoutId?.toString() ?: NEW_WORKOUT_TOKEN
            return "workoutEditor/$encodedId"
        }
    }

    data object ExerciseDetail : AppDestination("exerciseDetail/{workoutId}/{exerciseId}") {
        const val WORKOUT_ID_ARG = "workoutId"
        const val EXERCISE_ID_ARG = "exerciseId"

        fun route(workoutId: Long?, exerciseId: Long): String {
            val encodedWorkoutId = workoutId?.toString() ?: WorkoutEditor.NEW_WORKOUT_TOKEN
            return "exerciseDetail/$encodedWorkoutId/$exerciseId"
        }
    }

    data object ExerciseHistory : AppDestination("exerciseHistory/{exerciseId}") {
        const val EXERCISE_ID_ARG = "exerciseId"

        fun route(exerciseId: Long): String = "exerciseHistory/$exerciseId"
    }

    data object ExerciseProgress : AppDestination("exerciseProgress/{exerciseId}") {
        const val EXERCISE_ID_ARG = "exerciseId"

        fun route(exerciseId: Long): String = "exerciseProgress/$exerciseId"
    }

    data object ActiveWorkout : AppDestination("activeWorkout/{sessionId}") {
        const val SESSION_ID_ARG = "sessionId"

        fun route(sessionId: Long): String = "activeWorkout/$sessionId"
    }

    data object Summary : AppDestination("summary/{sessionId}") {
        const val SESSION_ID_ARG = "sessionId"

        fun route(sessionId: String): String = "summary/$sessionId"
    }
}
