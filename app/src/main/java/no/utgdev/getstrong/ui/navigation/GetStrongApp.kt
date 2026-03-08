package no.utgdev.getstrong.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController

@Composable
fun GetStrongApp() {
    val navController = rememberNavController()
    AppNavGraph(navController = navController)
}
