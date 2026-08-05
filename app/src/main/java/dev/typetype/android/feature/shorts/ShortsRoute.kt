package dev.typetype.android.feature.shorts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.feature.menu.rememberVideoMenuScope
import dev.typetype.android.feature.player.PlayerViewModel
import dev.typetype.android.feature.player.ShortsPlayerRoute
import dev.typetype.android.feature.player.components.LocalMediaController
import dev.typetype.android.feature.player.host.PlayerHostController
import dev.typetype.android.feature.player.host.PlayerHostTarget

@Composable
fun ShortsRoute(
    onPlayVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    playerHostController: PlayerHostController,
    playerViewModel: PlayerViewModel,
    viewModel: ShortsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playerHostState by playerHostController.state.collectAsStateWithLifecycle()
    val mediaController = LocalMediaController.current
    val menuScope = rememberVideoMenuScope(onOpenChannel)
    val visibleState = state.copy(videos = state.videos.filterNot(menuScope::isHidden))

    DisposableEffect(playerHostController) {
        onDispose { playerHostController.closeEmbeddedPlayback() }
    }
    LaunchedEffect(visibleState.hidden, visibleState.videos.isEmpty()) {
        if (visibleState.hidden || visibleState.videos.isEmpty()) {
            playerHostController.closeEmbeddedPlayback()
        }
    }

    ShortsScreen(
        state = visibleState,
        onPlayVideo = { url ->
            if (
                playerHostState.videoUrl == url &&
                playerHostState.target == PlayerHostTarget.Embedded
            ) {
                playerHostController.expand()
            } else {
                onPlayVideo(url)
            }
        },
        onOpenChannel = onOpenChannel,
        onRefresh = { viewModel.onAction(ShortsAction.Refresh) },
        onLoadMore = { viewModel.onAction(ShortsAction.LoadMore) },
        embeddedPlaybackEnabled = true,
        onActiveVideoChanged = { video ->
            if (video == null) {
                mediaController?.pause()
            } else {
                if (playerHostController.state.value.videoUrl != video.url) {
                    mediaController?.pause()
                }
                playerHostController.openEmbeddedVideo(video.url, state.autoplayEnabled)
            }
        },
        embeddedPlayback = { video, onAdvance ->
            if (
                playerHostState.videoUrl == video.url &&
                playerHostState.target == PlayerHostTarget.Embedded
            ) {
                ShortsPlayerRoute(
                    videoUrl = video.url,
                    viewModel = playerViewModel,
                    onAdvance = onAdvance,
                )
            }
        },
    )
}
