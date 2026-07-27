package dev.typetype.android

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import dev.typetype.android.core.ui.navigation.ProfileSettingsRoute
import dev.typetype.android.core.ui.navigation.ResetPasswordRoute
import dev.typetype.android.feature.settings.profile.ProfileSettingsRoute as ProfileSettingsRouteScreen
import dev.typetype.android.feature.setup.resetpassword.ResetPasswordRoute as ResetPasswordRouteScreen

internal fun NavGraphBuilder.profileDestinations(
    navController: NavHostController,
    currentServerId: String?,
) {
    composable<ProfileSettingsRoute> {
        ProfileSettingsRouteScreen(
            onNavigateBack = { navController.popBackStack() },
            onResetPassword = {
                currentServerId?.let { navController.navigate(ResetPasswordRoute(it)) }
            },
        )
    }
    composable<ResetPasswordRoute> {
        ResetPasswordRouteScreen(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}
