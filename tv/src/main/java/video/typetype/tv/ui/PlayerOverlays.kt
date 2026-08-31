package video.typetype.tv.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import video.typetype.sdk.core.PlaybackSession
import video.typetype.sdk.core.StreamDetails
import video.typetype.sdk.core.SubtitleTrack
import video.typetype.sdk.core.Video
import video.typetype.sdk.core.Comment
import video.typetype.tv.player.SponsorBlockPolicy
import video.typetype.tv.player.TvSponsorBlockSegment

@Composable
internal fun BoxScope.PlayerOverlays(
    video: Video,
    stream: StreamDetails,
    playback: PlaybackSession,
    supportedVideoItags: Set<Int>,
    subtitle: SubtitleTrack?,
    isAudioOnly: Boolean,
    controlsVisible: Boolean,
    showQueue: Boolean,
    tracksRequested: Boolean,
    commentsRequested: Boolean,
    commentsState: CommentsUiState,
    isPlaying: Boolean,
    positionMilliseconds: Long,
    durationMilliseconds: Long,
    isAdvancing: Boolean,
    serverError: String?,
    subtitleError: String?,
    sponsorBlockPolicy: SponsorBlockPolicy,
    activeSponsorSegment: TvSponsorBlockSegment?,
    playFocusRequester: FocusRequester,
    onSeekBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekForward: () -> Unit,
    onQueue: () -> Unit,
    onTracks: () -> Unit,
    onComments: () -> Unit,
    onSkipSponsor: (TvSponsorBlockSegment) -> Unit,
    onLoadMoreComments: () -> Unit,
    onLoadCommentReplies: (Comment) -> Unit,
    onPlayQueueItem: (Video) -> Unit,
    onSelectVideoTrack: (Int, Long) -> Unit,
    onSelectAudioTrack: (Int, String?, Long) -> Unit,
    onSelectSubtitle: (String?, Boolean, String?, Long) -> Unit,
    onDismissTracks: () -> Unit,
    onDismissComments: () -> Unit,
) {
    AnimatedVisibility(
        visible = sponsorBlockPolicy.showCurrentSegment && activeSponsorSegment != null &&
            !commentsRequested && !tracksRequested && !showQueue,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.TopEnd).padding(24.dp),
    ) {
        activeSponsorSegment?.let { segment ->
            SponsorBlockIndicator(
                segment = segment,
                canSkip = sponsorBlockPolicy.canManuallySkip(segment),
                onSkip = { onSkipSponsor(segment) },
            )
        }
    }
    subtitleError?.let { message ->
        Surface(
            modifier = Modifier.align(Alignment.TopCenter).padding(24.dp),
            colors = androidx.tv.material3.SurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = .96f),
            ),
        ) {
            Text(
                "Subtitles unavailable: $message",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
    AnimatedVisibility(
        visible = controlsVisible && !showQueue && !tracksRequested && !commentsRequested,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        PlaybackControls(
            video = video,
            isPlaying = isPlaying,
            positionMilliseconds = positionMilliseconds,
            durationMilliseconds = durationMilliseconds,
            hasQueue = stream.relatedStreams.isNotEmpty(),
            hasTracks = !isAudioOnly &&
                (stream.videoOnlyStreams.isNotEmpty() || stream.audioStreams.isNotEmpty() || playback.subtitles.isNotEmpty()),
            hasComments = !commentsState.disabled,
            sponsorBlockPolicy = sponsorBlockPolicy,
            activeSponsorSegment = activeSponsorSegment,
            playFocusRequester = playFocusRequester,
            onSeekBack = onSeekBack,
            onPlayPause = onPlayPause,
            onSeekForward = onSeekForward,
            onQueue = onQueue,
            onTracks = onTracks,
            onComments = onComments,
            onSkipSponsor = onSkipSponsor,
        )
    }
    AnimatedVisibility(
        visible = commentsRequested,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.CenterEnd),
    ) {
        CommentsPanel(
            state = commentsState,
            onLoadReplies = onLoadCommentReplies,
            onLoadMore = onLoadMoreComments,
            onDismiss = onDismissComments,
        )
    }
    AnimatedVisibility(
        visible = tracksRequested && !isAudioOnly,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.CenterEnd).padding(32.dp),
    ) {
        PlaybackTrackPanel(
            stream = stream,
            playback = playback,
            supportedVideoItags = supportedVideoItags,
            selectedVideoItag = playback.selectedVideoItag,
            selectedAudioItag = playback.selectedAudioItag,
            selectedAudioTrackId = playback.audioTrackId,
            subtitle = subtitle,
            onVideoTrack = { onSelectVideoTrack(it, positionMilliseconds) },
            onAudioTrack = { itag, trackId -> onSelectAudioTrack(itag, trackId, positionMilliseconds) },
            onSubtitle = { language, auto, name -> onSelectSubtitle(language, auto, name, positionMilliseconds) },
            onDismiss = onDismissTracks,
        )
    }
    AnimatedVisibility(
        visible = showQueue,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        PlaybackQueue(
            videos = stream.relatedStreams,
            isAdvancing = isAdvancing,
            error = serverError,
            onPlay = onPlayQueueItem,
        )
    }
}
