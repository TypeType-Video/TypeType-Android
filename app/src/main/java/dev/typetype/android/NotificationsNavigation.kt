package dev.typetype.android

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import dev.typetype.android.core.ui.navigation.NotificationsRoute
import dev.typetype.android.feature.notifications.NotificationsRoute as NotificationsRouteScreen

internal fun NavGraphBuilder.notificationsDestination(
    navController: NavHostController,
    onPlayVideo: (String) -> Unit,
) {
    composable<NotificationsRoute> {
        NotificationsRouteScreen(
            onNavigateBack = { navController.popBackStack() },
            onPlayVideo = onPlayVideo,
        )
    }
}
