package dev.typetype.android

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.typetype.android.core.ui.navigation.HomeRoute
import dev.typetype.android.feature.home.HomePlaceholderScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> { HomePlaceholderScreen() }
    }
}
