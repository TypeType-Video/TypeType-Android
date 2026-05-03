package dev.typetype.android

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.typetype.android.core.ui.navigation.AddServerRoute
import dev.typetype.android.core.ui.navigation.HomeRoute
import dev.typetype.android.core.ui.navigation.WelcomeRoute
import dev.typetype.android.feature.home.HomePlaceholderScreen
import dev.typetype.android.feature.setup.welcome.WelcomeRoute as WelcomeRouteScreen

@Composable
fun AppNavHost(startRoute: Any) {
    val navController: NavHostController = rememberNavController()
    NavHost(navController = navController, startDestination = startRoute) {
        composable<WelcomeRoute> {
            WelcomeRouteScreen(
                onNavigateToAddServer = { navController.navigate(AddServerRoute) },
            )
        }
        composable<AddServerRoute> {
            // AddServer screen lands in 2.3
        }
        composable<HomeRoute> { HomePlaceholderScreen() }
    }
}
