package dev.typetype.android

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import dev.typetype.android.core.ui.navigation.LibraryRoute
import dev.typetype.android.core.ui.navigation.PlaylistRoute
import dev.typetype.android.core.ui.navigation.PublicPlaylistRoute
import dev.typetype.android.feature.library.LibraryRoute as LibraryRouteScreen

internal fun NavGraphBuilder.libraryDestination(
    navController: NavHostController,
    onPlayVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
) {
    composable<LibraryRoute> {
        LibraryRouteScreen(
            onPlayVideo = onPlayVideo,
            onOpenPlaylist = { playlistId ->
                navController.navigate(PlaylistRoute(playlistId = playlistId))
            },
            onOpenPublicPlaylist = { playlistUrl ->
                navController.navigate(PublicPlaylistRoute(playlistUrl)) {
                    launchSingleTop = true
                }
            },
            onOpenChannel = onOpenChannel,
        )
    }
}
