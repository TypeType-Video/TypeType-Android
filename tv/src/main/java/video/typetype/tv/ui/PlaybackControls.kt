package video.typetype.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import video.typetype.sdk.core.Video
import video.typetype.tv.player.SponsorBlockPolicy
import video.typetype.tv.player.TvSponsorBlockSegment

@Composable
internal fun PlaybackControls(
    video: Video,
    isPlaying: Boolean,
    positionMilliseconds: Long,
    durationMilliseconds: Long,
    hasQueue: Boolean,
    hasTracks: Boolean,
    hasComments: Boolean,
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
) {
    val backFocus = remember { FocusRequester() }
    val forwardFocus = remember { FocusRequester() }
    val queueFocus = remember { FocusRequester() }
    val optionsFocus = remember { FocusRequester() }
    val commentsFocus = remember { FocusRequester() }
    val sponsorFocus = remember { FocusRequester() }
    val sponsorAction = activeSponsorSegment?.takeIf(sponsorBlockPolicy::canManuallySkip)
    val progress = if (durationMilliseconds > 0L) {
        (positionMilliseconds.toFloat() / durationMilliseconds).coerceIn(0f, 1f)
    } else 0f
    Column(
        modifier = Modifier.fillMaxWidth().background(
            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .9f), Color.Black)),
        ).padding(start = 58.dp, end = 58.dp, top = 62.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                video.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${formatPlaybackTime(positionMilliseconds)}  /  ${formatPlaybackTime(durationMilliseconds)}",
                color = Color.White.copy(alpha = .72f),
            )
        }
        Text(video.uploaderName, color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.bodyMedium)
        SponsorBlockProgressBar(
            progress = progress,
            durationMilliseconds = durationMilliseconds,
            segments = sponsorBlockPolicy.visibleSegments,
            showSegments = sponsorBlockPolicy.showChapters,
            modifier = Modifier.fillMaxWidth().height(5.dp),
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ControlButton(
                    Icons.Default.Replay10,
                    "Back 10s",
                    onSeekBack,
                    Modifier.focusRequester(backFocus).focusProperties {
                        left = FocusRequester.Cancel
                        right = playFocusRequester
                    },
                )
                ControlButton(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (isPlaying) "Pause" else "Play",
                    onPlayPause,
                    Modifier.focusRequester(playFocusRequester).focusProperties {
                        left = backFocus
                        right = forwardFocus
                    },
                )
                ControlButton(
                    Icons.Default.Forward10,
                    "Forward 10s",
                    onSeekForward,
                    Modifier.focusRequester(forwardFocus).focusProperties {
                        left = playFocusRequester
                        right = when {
                            sponsorAction != null -> sponsorFocus
                            hasQueue -> queueFocus
                            hasTracks -> optionsFocus
                            hasComments -> commentsFocus
                            else -> FocusRequester.Cancel
                        }
                    },
                )
                if (sponsorAction != null) ControlButton(
                    Icons.Default.SkipNext,
                    "Skip",
                    { onSkipSponsor(sponsorAction) },
                    Modifier.focusRequester(sponsorFocus).focusProperties {
                        left = forwardFocus
                        right = when {
                            hasQueue -> queueFocus
                            hasTracks -> optionsFocus
                            hasComments -> commentsFocus
                            else -> FocusRequester.Cancel
                        }
                    },
                )
                if (hasQueue) ControlButton(
                    Icons.Default.QueuePlayNext,
                    "Up next",
                    onQueue,
                    Modifier.focusRequester(queueFocus).focusProperties {
                        left = if (sponsorAction != null) sponsorFocus else forwardFocus
                        right = when {
                            hasTracks -> optionsFocus
                            hasComments -> commentsFocus
                            else -> FocusRequester.Cancel
                        }
                    },
                )
                if (hasTracks) ControlButton(
                    Icons.Default.Tune,
                    "Options",
                    onTracks,
                    Modifier.focusRequester(optionsFocus).focusProperties {
                        left = when {
                            hasQueue -> queueFocus
                            sponsorAction != null -> sponsorFocus
                            else -> forwardFocus
                        }
                        right = if (hasComments) commentsFocus else FocusRequester.Cancel
                    },
                )
                if (hasComments) ControlButton(
                    Icons.AutoMirrored.Filled.Comment,
                    "Comments",
                    onComments,
                    Modifier.focusRequester(commentsFocus).focusProperties {
                        left = when {
                            hasTracks -> optionsFocus
                            hasQueue -> queueFocus
                            sponsorAction != null -> sponsorFocus
                            else -> forwardFocus
                        }
                        right = FocusRequester.Cancel
                    },
                )
            }
        }
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(modifier = modifier, onClick = onClick) {
        Icon(icon, contentDescription = label)
        Spacer(Modifier.width(7.dp))
        Text(label)
    }
}

private fun formatPlaybackTime(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0L) / 1_000L
    val hours = seconds / 3_600L
    val minutes = seconds % 3_600L / 60L
    val remainingSeconds = seconds % 60L
    return if (hours > 0L) "%d:%02d:%02d".format(hours, minutes, remainingSeconds)
    else "%d:%02d".format(minutes, remainingSeconds)
}
