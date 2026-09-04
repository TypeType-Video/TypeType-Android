package dev.typetype.android.feature.menu

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.LocalAppSnackbarHost
import dev.typetype.android.core.ui.components.VideoMenuAction
import dev.typetype.android.core.ui.components.VideoMenuItemState
import dev.typetype.android.core.ui.share.LocalServerBaseUrl
import dev.typetype.android.core.ui.share.ShareChooserSheet
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.navigation.canonicalVideoIdentity
import dev.typetype.android.domain.actions.titleMatchesBlockedKeyword
import dev.typetype.android.feature.download.DownloadSelectionSheet
import dev.typetype.android.feature.player.components.PlaylistPickerSheet

@Stable
class VideoMenuScope internal constructor(
    val watchedUrls: Set<String>,
    val blockedVideoUrls: Set<String>,
    val blockedChannelUrls: Set<String>,
    val blockedKeywords: Set<String>,
    private val favorites: Set<String>,
    private val watchLater: Set<String>,
    val onAction: (VideoMenuAction, Video) -> Unit,
) {
    fun stateFor(video: Video): VideoMenuItemState = VideoMenuItemState(
        isFavorite = canonicalVideoIdentity(video.url) in favorites,
        isInWatchLater = canonicalVideoIdentity(video.url) in watchLater,
        isWatched = canonicalVideoIdentity(video.url) in watchedUrls,
    )

    fun isHidden(video: Video): Boolean =
        canonicalVideoIdentity(video.url) in blockedVideoUrls ||
            video.uploaderUrl in blockedChannelUrls ||
            titleMatchesBlockedKeyword(video.title, blockedKeywords)
}

@Composable
fun rememberVideoMenuScope(
    onOpenChannel: (channelUrl: String) -> Unit,
): VideoMenuScope {
    val viewModel: VideoMenuHandlerViewModel = hiltViewModel()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteUrls.collectAsStateWithLifecycle()
    val watchLater by viewModel.watchLaterUrls.collectAsStateWithLifecycle()
    val watched by viewModel.watchedUrls.collectAsStateWithLifecycle()
    val blockedVideos by viewModel.blockedVideoUrls.collectAsStateWithLifecycle()
    val blockedChannels by viewModel.blockedChannelUrls.collectAsStateWithLifecycle()
    val blockedKeywords by viewModel.blockedKeywords.collectAsStateWithLifecycle()
    val serverBaseUrl = LocalServerBaseUrl.current

    var pickerVideo by remember { mutableStateOf<Video?>(null) }
    var downloadVideo by remember { mutableStateOf<Video?>(null) }
    var shareVideoUrl by remember { mutableStateOf<String?>(null) }

    val effectiveHost: SnackbarHostState =
        LocalAppSnackbarHost.current ?: remember { SnackbarHostState() }
    LaunchedEffect(viewModel, effectiveHost) {
        viewModel.events.collect { event ->
            when (event) {
                is VideoMenuEvent.Snackbar -> effectiveHost.showSnackbar(
                    event.message,
                    duration = SnackbarDuration.Short,
                )
            }
        }
    }

    shareVideoUrl?.let { videoUrl ->
        ShareChooserSheet(
            serverBaseUrl = serverBaseUrl,
            videoUrl = videoUrl,
            onDismiss = { shareVideoUrl = null },
        )
    }

    pickerVideo?.let { video ->
        PlaylistPickerSheet(
            playlists = playlists,
            isInFlight = false,
            onAddToPlaylist = { id ->
                viewModel.addToPlaylist(id, video)
                pickerVideo = null
            },
            onCreatePlaylist = { name ->
                viewModel.createPlaylistAndAdd(name, video)
                pickerVideo = null
            },
            onDismiss = { pickerVideo = null },
        )
    }

    downloadVideo?.let { video ->
        DownloadSelectionSheet(
            isInFlight = false,
            onSelect = { selection ->
                viewModel.download(video, selection)
                downloadVideo = null
            },
            onDismiss = { downloadVideo = null },
        )
    }

    val onAction: (VideoMenuAction, Video) -> Unit = { action, video ->
        when (action) {
            VideoMenuAction.PlayNext -> viewModel.playNext(video)
            VideoMenuAction.AddToQueue -> viewModel.addToQueue(video)
            VideoMenuAction.ToggleFavorite -> viewModel.toggleFavorite(
                video,
                canonicalVideoIdentity(video.url) in favorites,
            )
            VideoMenuAction.ToggleWatchLater -> viewModel.toggleWatchLater(
                video,
                canonicalVideoIdentity(video.url) in watchLater,
            )
            VideoMenuAction.AddToPlaylist -> pickerVideo = video
            VideoMenuAction.ToggleWatched -> viewModel.toggleWatched(
                video,
                canonicalVideoIdentity(video.url) in watched,
            )
            VideoMenuAction.Download -> downloadVideo = video
            VideoMenuAction.Share -> shareVideoUrl = video.url
            VideoMenuAction.OpenChannel -> onOpenChannel(video.uploaderUrl)
            VideoMenuAction.BlockVideo -> viewModel.blockVideo(video)
            VideoMenuAction.BlockChannel -> viewModel.blockChannel(video)
        }
    }

    return remember(favorites, watchLater, watched, blockedVideos, blockedChannels, blockedKeywords) {
        VideoMenuScope(
            watchedUrls = watched,
            blockedVideoUrls = blockedVideos,
            blockedChannelUrls = blockedChannels,
            blockedKeywords = blockedKeywords,
            favorites = favorites,
            watchLater = watchLater,
            onAction = onAction,
        )
    }
}
