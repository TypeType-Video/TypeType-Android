package dev.typetype.android.feature.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import dev.typetype.android.R
import androidx.media3.session.MediaController
import dev.typetype.android.domain.stream.SponsorBlockSegment
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.feature.player.state.ResizeMode

@Composable
internal fun PlayerSurfaceChrome(
    player: MediaController,
    stream: Stream,
    title: String,
    sponsorBlockSegments: List<SponsorBlockSegment>,
    seekPreviewPositionMs: Long?,
    seekDragOverlayVisible: Boolean,
    seekDragPositionMs: Long,
    fineSeeking: Boolean,
    isFullscreen: Boolean,
    isInPip: Boolean,
    controlsAllowedByProgress: Boolean,
    controlsVisible: Boolean,
    accessibleControls: Boolean,
    acceptsInput: Boolean,
    timelineScrubbing: Boolean,
    onTimelineScrubbingChange: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenOptions: () -> Unit,
    onOpenChapters: () -> Unit,
    onEnterPip: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onCycleResizeMode: () -> Unit,
    resizeMode: ResizeMode,
    isPipAvailable: Boolean,
    chaptersAvailable: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = seekDragOverlayVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SeekStoryboardPreview(
                    frame = stream.storyboard?.frameAt(seekDragPositionMs),
                    timecode = if (fineSeeking) {
                        stringResource(
                            R.string.player_fine_seek_position,
                            formatPlayerTime(seekDragPositionMs),
                        )
                    } else {
                        formatPlayerTime(seekDragPositionMs)
                    },
                )
                PlayerSeekScrubOverlay(
                    player = player,
                    positionMs = seekDragPositionMs,
                    segments = sponsorBlockSegments,
                    isFullscreen = isFullscreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isFullscreen) {
                                Modifier.padding(start = 12.dp, end = 8.dp, bottom = 6.dp)
                            } else {
                                Modifier.padding(start = 4.dp, end = 4.dp)
                            },
                        ),
                )
            }
        }

        AnimatedVisibility(
            visible = controlsAllowedByProgress &&
                (controlsVisible || accessibleControls) &&
                !isInPip && acceptsInput && !seekDragOverlayVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            PlayerControls(
                player = player,
                storyboard = stream.storyboard,
                title = title,
                onNavigateBack = onNavigateBack,
                onOpenOptions = onOpenOptions,
                onOpenChapters = onOpenChapters,
                onEnterPip = onEnterPip,
                onToggleFullscreen = onToggleFullscreen,
                onCycleResizeMode = onCycleResizeMode,
                resizeMode = resizeMode,
                isFullscreen = isFullscreen,
                isPipAvailable = isPipAvailable,
                chaptersAvailable = chaptersAvailable,
                sponsorBlockSegments = sponsorBlockSegments,
                seekPreviewPositionMs = seekPreviewPositionMs,
                onTimelineScrubbingChange = onTimelineScrubbingChange,
                timelineScrubbing = timelineScrubbing,
                modifier = modifier.fillMaxSize(),
            )
        }
    }
}
