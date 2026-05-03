package dev.typetype.android

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.typetype.android.core.ui.navigation.AddServerRoute
import dev.typetype.android.core.ui.navigation.HomeRoute
import dev.typetype.android.core.ui.navigation.LoginRoute
import dev.typetype.android.core.ui.navigation.WelcomeRoute
import dev.typetype.android.feature.home.HomeRoute as HomeRouteScreen
import dev.typetype.android.feature.setup.addserver.AddServerRoute as AddServerRouteScreen
import dev.typetype.android.feature.setup.login.LoginRoute as LoginRouteScreen
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
            AddServerRouteScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = { serverId ->
                    navController.navigate(LoginRoute(serverId = serverId)) {
                        popUpTo(WelcomeRoute) { inclusive = false }
                    }
                },
            )
        }
        composable<LoginRoute> {
            LoginRouteScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(HomeRoute) {
                        popUpTo(WelcomeRoute) { inclusive = true }
                    }
                },
            )
        }
        composable<HomeRoute> { HomeRouteScreen() }
    }
}
