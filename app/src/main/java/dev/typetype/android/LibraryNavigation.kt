package dev.typetype.android

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.typetype.android.core.ui.navigation.LibraryLandingRoute
import dev.typetype.android.core.ui.navigation.LibraryRoute
import dev.typetype.android.core.ui.navigation.PlaylistRoute
import dev.typetype.android.core.ui.navigation.PublicPlaylistRoute
import dev.typetype.android.feature.library.LibraryRoute as LibraryRouteScreen
import dev.typetype.android.feature.library.LibraryTab

internal fun NavGraphBuilder.libraryDestination(
    navController: NavHostController,
    onPlayVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
) {
    composable<LibraryRoute> {
        LibraryScreenDestination(navController, onPlayVideo, onOpenChannel)
    }
    composable<LibraryLandingRoute> { entry ->
        LibraryScreenDestination(
            navController = navController,
            onPlayVideo = onPlayVideo,
            onOpenChannel = onOpenChannel,
            initialTab = libraryTabForLanding(entry.toRoute<LibraryLandingRoute>().tab),
        )
    }
}

private fun libraryTabForLanding(value: String?): LibraryTab = when (value) {
    "favorites" -> LibraryTab.Favorites
    "watch-later" -> LibraryTab.WatchLater
    "playlists" -> LibraryTab.Playlists
    else -> LibraryTab.History
}

@Composable
private fun LibraryScreenDestination(
    navController: NavHostController,
    onPlayVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    initialTab: LibraryTab? = null,
) {
    LibraryRouteScreen(
        initialTab = initialTab,
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
