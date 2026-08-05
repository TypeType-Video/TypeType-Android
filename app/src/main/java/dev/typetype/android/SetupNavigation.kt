package dev.typetype.android

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import dev.typetype.android.core.ui.navigation.AddServerRoute
import dev.typetype.android.core.ui.navigation.HomeRoute
import dev.typetype.android.core.ui.navigation.LoginRoute
import dev.typetype.android.core.ui.navigation.RegisterRoute
import dev.typetype.android.core.ui.navigation.ResetPasswordRoute
import dev.typetype.android.core.ui.navigation.WelcomeRoute
import dev.typetype.android.feature.setup.addserver.AddServerRoute as AddServerRouteScreen
import dev.typetype.android.feature.setup.login.LoginRoute as LoginRouteScreen
import dev.typetype.android.feature.setup.register.RegisterRoute as RegisterRouteScreen
import dev.typetype.android.feature.setup.welcome.WelcomeRoute as WelcomeRouteScreen

internal fun NavGraphBuilder.setupDestinations(
    navController: NavHostController,
    mainViewModel: MainViewModel,
) {
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
            onNavigateToResetPassword = {
                navController.navigate(ResetPasswordRoute(it))
            },
            onNavigateToRegister = {
                navController.navigate(RegisterRoute(it))
            },
            onNavigateToHome = {
                activateAccount(navController, mainViewModel)
            },
        )
    }
    composable<RegisterRoute> {
        RegisterRouteScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToHome = {
                activateAccount(navController, mainViewModel)
            },
        )
    }
}

private fun activateAccount(
    navController: NavHostController,
    mainViewModel: MainViewModel,
) {
    mainViewModel.onAccountActivated()
    navController.navigate(HomeRoute) {
        popUpTo(0) { inclusive = true }
    }
}
