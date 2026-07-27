package dev.typetype.android

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import dev.typetype.android.core.ui.navigation.ChannelRoute
import dev.typetype.android.core.ui.navigation.PodcastRoute
import dev.typetype.android.feature.channel.ChannelRoute as ChannelRouteScreen
import dev.typetype.android.feature.player.host.PlayerHostController
import dev.typetype.android.feature.podcast.PodcastRoute as PodcastRouteScreen

internal fun NavGraphBuilder.channelAndPodcastDestinations(
    navController: NavHostController,
    playerHostController: PlayerHostController,
    onPlayVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
) {
    composable<ChannelRoute> {
        ChannelRouteScreen(
            onNavigateBack = { navController.popBackStack() },
            onPlayVideo = onPlayVideo,
            onOpenPodcast = { podcastUrl ->
                navController.navigate(PodcastRoute(podcastUrl)) { launchSingleTop = true }
            },
        )
    }
    composable<PodcastRoute> {
        PodcastRouteScreen(
            onNavigateBack = { navController.popBackStack() },
            onPlayVideo = onPlayVideo,
            onPlayQueue = { title, episodes, shuffle ->
                playerHostController.openQueue(
                    title = title,
                    entries = episodes.map { it.toPlaybackQueueEntry() },
                    shuffle = shuffle,
                )
            },
            onOpenChannel = onOpenChannel,
        )
    }
}
