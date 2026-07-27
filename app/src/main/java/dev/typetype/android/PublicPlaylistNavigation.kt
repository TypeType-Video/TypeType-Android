package dev.typetype.android

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import dev.typetype.android.core.ui.navigation.PublicPlaylistRoute
import dev.typetype.android.feature.publicplaylist.PublicPlaylistRoute as PublicPlaylistRouteScreen
import dev.typetype.android.feature.player.host.PlayerHostController

internal fun NavGraphBuilder.publicPlaylistDestination(
    navController: NavHostController,
    playerHostController: PlayerHostController,
    onPlayVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
) {
    composable<PublicPlaylistRoute> {
        PublicPlaylistRouteScreen(
            onNavigateBack = { navController.popBackStack() },
            onPlayVideo = onPlayVideo,
            onPlayQueue = { title, videos, shuffle ->
                playerHostController.openQueue(
                    title = title,
                    entries = videos.map { it.toPlaybackQueueEntry() },
                    shuffle = shuffle,
                )
            },
            onOpenChannel = onOpenChannel,
        )
    }
}
