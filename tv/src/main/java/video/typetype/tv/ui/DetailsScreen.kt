package video.typetype.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEnterExitScope
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import video.typetype.sdk.core.StreamDetails
import video.typetype.sdk.core.Video
import video.typetype.sdk.core.Comment
import video.typetype.sdk.core.UserPlaylist
import video.typetype.sdk.core.selectSabrPlaybackTracks
import video.typetype.sdk.core.DownloadJob
import video.typetype.tv.data.TvDownloadOption
import video.typetype.tv.data.buildTvDownloadOptions
import video.typetype.tv.ui.theme.LocalTvAppearance

@Composable
@OptIn(ExperimentalComposeUiApi::class)
internal fun DetailsScreen(
    video: Video,
    stream: StreamDetails?,
    isLoading: Boolean,
    errorMessage: String?,
    isAuthenticated: Boolean,
    isFavorite: Boolean,
    isInWatchLater: Boolean,
    isSubscribed: Boolean,
    isActionInProgress: Boolean,
    playlists: List<UserPlaylist>,
    downloadJob: DownloadJob?,
    isSavingDownload: Boolean,
    downloadMessage: String?,
    downloadError: String?,
    onPlay: () -> Unit,
    onPlayAudio: () -> Unit,
    onOpenRelated: (Video) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleWatchLater: () -> Unit,
    onTogglePlaylistVideo: (UserPlaylist, Video) -> Unit,
    onStartDownload: (TvDownloadOption) -> Unit,
    onCancelDownload: () -> Unit,
    onRetryDownloadArtifact: () -> Unit,
    onClearDownload: () -> Unit,
    onOpenChannel: () -> Unit,
    onToggleSubscription: () -> Unit,
    commentsState: CommentsUiState,
    onLoadMoreComments: () -> Unit,
    onLoadCommentReplies: (Comment) -> Unit,
    selectedVideoItag: Int?,
    supportedVideoItags: Set<Int>,
    selectedAudioItag: Int?,
    selectedAudioTrackId: String?,
    selectedSubtitleLanguage: String?,
    selectedSubtitleAuto: Boolean,
    selectedSubtitleName: String?,
    onSelectVideoTrack: (Int) -> Unit,
    onSelectAudioTrack: (Int, String?) -> Unit,
    onSelectSubtitle: (String?, Boolean, String?) -> Unit,
    onBack: () -> Unit,
) {
    var showPlaybackOptions by remember(video.id) { mutableStateOf(false) }
    var showComments by remember(video.id) { mutableStateOf(false) }
    var showSave by remember(video.id) { mutableStateOf(false) }
    var showDownload by remember(video.id) { mutableStateOf(false) }
    val playFocusRequester = remember { FocusRequester() }
    val saveFocusRequester = remember { FocusRequester() }
    val downloadFocusRequester = remember { FocusRequester() }
    val playbackFocusRequester = remember { FocusRequester() }
    val commentsFocusRequester = remember { FocusRequester() }
    BackHandler(enabled = !showPlaybackOptions && !showDownload) {
        when {
            showComments -> {
                showComments = false
                commentsFocusRequester.requestFocus()
            }
            showSave -> {
                showSave = false
                saveFocusRequester.requestFocus()
            }
            else -> onBack()
        }
    }
    LaunchedEffect(video.id) {
        playFocusRequester.requestFocus()
    }
    Box(
        modifier = Modifier.fillMaxSize().focusProperties {
            onExit = { scope: FocusEnterExitScope -> scope.cancelFocusChange() }
        }.focusGroup(),
    ) {
        CinematicBackdrop(video, Modifier.fillMaxSize())
        DetailsHero(
            video = video,
            stream = stream,
            isLoading = isLoading,
            errorMessage = errorMessage,
            isAuthenticated = isAuthenticated,
            isFavorite = isFavorite,
            isSubscribed = isSubscribed,
            isActionInProgress = isActionInProgress,
            playFocusRequester = playFocusRequester,
            saveFocusRequester = saveFocusRequester,
            downloadFocusRequester = downloadFocusRequester,
            playbackFocusRequester = playbackFocusRequester,
            commentsFocusRequester = commentsFocusRequester,
            selectedVideoItag = selectedVideoItag,
            selectedAudioItag = selectedAudioItag,
            selectedAudioTrackId = selectedAudioTrackId,
            onPlay = onPlay,
            onRetry = { onOpenRelated(video) },
            onPlayAudio = onPlayAudio,
            onToggleFavorite = onToggleFavorite,
            onShowSave = { showSave = true },
            onShowDownload = { showDownload = true },
            onOpenChannel = onOpenChannel,
            onToggleSubscription = onToggleSubscription,
            onShowComments = { showComments = true },
            onShowPlaybackOptions = { showPlaybackOptions = true },
        )
        stream?.relatedStreams?.takeIf { it.isNotEmpty() }?.let { related ->
            Box(Modifier.align(Alignment.BottomStart)) {
                VideoRow("More like this", related, onOpenRelated)
            }
        }
        if (showPlaybackOptions && stream != null) {
            DetailsPlaybackPanel(
                stream = stream,
                supportedVideoItags = supportedVideoItags,
                selectedVideoItag = selectedVideoItag,
                selectedAudioItag = selectedAudioItag,
                selectedAudioTrackId = selectedAudioTrackId,
                selectedSubtitleLanguage = selectedSubtitleLanguage,
                selectedSubtitleAuto = selectedSubtitleAuto,
                selectedSubtitleName = selectedSubtitleName,
                onSelectVideoTrack = onSelectVideoTrack,
                onSelectAudioTrack = onSelectAudioTrack,
                onSelectSubtitle = onSelectSubtitle,
                onDismiss = {
                    showPlaybackOptions = false
                    playbackFocusRequester.requestFocus()
                },
            )
        }
        val transitionMillis = LocalTvAppearance.current.transitionMillis
        AnimatedVisibility(
            visible = showComments,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(transitionMillis)),
            exit = fadeOut(tween(transitionMillis)),
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .48f)))
        }
        AnimatedVisibility(
            visible = showComments,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally(tween(transitionMillis)) { it } + fadeIn(tween(transitionMillis)),
            exit = slideOutHorizontally(tween(transitionMillis)) { it } + fadeOut(tween(transitionMillis)),
        ) {
            Box {
                CommentsPanel(
                    state = commentsState,
                    onLoadReplies = onLoadCommentReplies,
                    onLoadMore = onLoadMoreComments,
                    onDismiss = {
                        showComments = false
                        commentsFocusRequester.requestFocus()
                    },
                )
            }
        }
        if (showSave) {
            Box(Modifier.align(Alignment.CenterEnd)) {
                SavePanel(video, playlists, isInWatchLater, onToggleWatchLater, onTogglePlaylistVideo) {
                    showSave = false
                    saveFocusRequester.requestFocus()
                }
            }
        }
        AnimatedVisibility(
            visible = showDownload,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(transitionMillis)),
            exit = fadeOut(tween(transitionMillis)),
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .48f)))
        }
        AnimatedVisibility(
            visible = showDownload,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally(tween(transitionMillis)) { it } + fadeIn(tween(transitionMillis)),
            exit = slideOutHorizontally(tween(transitionMillis)) { it } + fadeOut(tween(transitionMillis)),
        ) {
            DownloadPanel(
                options = remember(stream, video.url) {
                    stream?.let { buildTvDownloadOptions(it, video.url) }.orEmpty()
                },
                job = downloadJob,
                isSaving = isSavingDownload,
                message = downloadMessage,
                error = downloadError,
                onStart = onStartDownload,
                onCancel = onCancelDownload,
                onRetryArtifact = onRetryDownloadArtifact,
                onClear = onClearDownload,
                onDismiss = {
                    showDownload = false
                    downloadFocusRequester.requestFocus()
                },
            )
        }
    }
}
