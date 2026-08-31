package video.typetype.tv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Download
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import video.typetype.sdk.core.StreamDetails
import video.typetype.sdk.core.Video
import video.typetype.sdk.core.selectSabrPlaybackTracks

@Composable
internal fun DetailsHero(
    video: Video,
    stream: StreamDetails?,
    isLoading: Boolean,
    errorMessage: String?,
    isAuthenticated: Boolean,
    isFavorite: Boolean,
    isSubscribed: Boolean,
    isActionInProgress: Boolean,
    playFocusRequester: FocusRequester,
    saveFocusRequester: FocusRequester,
    downloadFocusRequester: FocusRequester,
    playbackFocusRequester: FocusRequester,
    commentsFocusRequester: FocusRequester,
    selectedVideoItag: Int?,
    selectedAudioItag: Int?,
    selectedAudioTrackId: String?,
    onPlay: () -> Unit,
    onRetry: () -> Unit,
    onPlayAudio: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShowSave: () -> Unit,
    onShowDownload: () -> Unit,
    onOpenChannel: () -> Unit,
    onToggleSubscription: () -> Unit,
    onShowComments: () -> Unit,
    onShowPlaybackOptions: () -> Unit,
) {
    val selected = stream?.selectSabrPlaybackTracks(selectedVideoItag, selectedAudioItag, selectedAudioTrackId)
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 58.dp, top = 36.dp, end = 42.dp).height(620.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Column(modifier = Modifier.widthIn(max = 600.dp)) {
            DetailsChannelRow(
                video, stream, isAuthenticated, isSubscribed, isActionInProgress,
                onOpenChannel, onToggleSubscription,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                video.title,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, lineHeight = 38.sp),
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                detailsMetadata(video, stream),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = .72f),
            )
            stream?.let { DetailsEngagement(it) }
            selected?.let {
                Spacer(Modifier.height(5.dp))
                Text(
                    listOfNotNull(
                        it.video.resolution,
                        playbackCodecLabel(it.video.codec),
                        it.audio.audioTrackName ?: it.audio.audioLocale ?: "Audio",
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = .72f),
                )
            }
            (stream?.description ?: video.shortDescription)?.takeIf(String::isNotBlank)?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White.copy(alpha = .88f),
                )
            }
            errorMessage?.let {
                val lines = it.lineSequence().toList()
                Spacer(Modifier.height(10.dp))
                Text(
                    lines.first(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                lines.drop(1).takeIf { diagnostics -> diagnostics.isNotEmpty() }?.let { diagnostics ->
                    Text(
                        diagnostics.joinToString(" "),
                        color = MaterialTheme.colorScheme.error.copy(alpha = .68f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PrimaryPlayButton(isLoading, stream != null, playFocusRequester, onPlay, onRetry)
            if (stream != null) {
                Button(
                    modifier = Modifier.width(154.dp),
                    onClick = onPlayAudio,
                    enabled = !isLoading && !stream.isLive,
                    colors = detailsSecondaryButtonColors(),
                ) { DetailsActionContent(Icons.Default.Headphones, "Audio only") }
            }
            if (stream != null) {
                Button(
                    modifier = Modifier.width(150.dp).focusRequester(downloadFocusRequester),
                    onClick = onShowDownload,
                    colors = detailsSecondaryButtonColors(),
                ) { DetailsActionContent(Icons.Default.Download, "Download") }
                Button(
                    modifier = Modifier.width(150.dp).focusRequester(playbackFocusRequester),
                    onClick = onShowPlaybackOptions,
                    colors = detailsSecondaryButtonColors(),
                ) { DetailsActionContent(Icons.Default.Settings, "Playback") }
            }
            Button(
                modifier = Modifier.width(150.dp).focusRequester(commentsFocusRequester),
                onClick = onShowComments,
                colors = detailsSecondaryButtonColors(),
            ) { DetailsActionContent(Icons.AutoMirrored.Filled.Comment, "Comments") }
        }
        if (isAuthenticated) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    modifier = Modifier.width(122.dp).focusRequester(saveFocusRequester),
                    onClick = onShowSave,
                    enabled = !isActionInProgress,
                    colors = detailsSecondaryButtonColors(),
                ) { DetailsActionContent(Icons.AutoMirrored.Filled.PlaylistAdd, "Save") }
                Button(
                    modifier = Modifier.width(154.dp),
                    onClick = onToggleFavorite,
                    enabled = !isActionInProgress,
                    colors = detailsSecondaryButtonColors(),
                ) {
                    DetailsActionContent(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        if (isFavorite) "Favorite" else "Add favorite",
                    )
                }
            }
        }
    }
}

@Composable
private fun detailsSecondaryButtonColors() = ButtonDefaults.colors(
    containerColor = Color(0xCC23262B),
    contentColor = Color.White,
    focusedContainerColor = MaterialTheme.colorScheme.primary,
    focusedContentColor = MaterialTheme.colorScheme.onPrimary,
    disabledContainerColor = Color(0x6623262B),
    disabledContentColor = Color.White.copy(alpha = .42f),
)

@Composable
private fun DetailsEngagement(stream: StreamDetails) {
    val values = listOfNotNull(
        stream.likeCount.takeIf { it >= 0L }?.let { Icons.Default.ThumbUp to compactCount(it) },
        stream.dislikeCount.takeIf { it >= 0L }?.let { Icons.Default.ThumbDown to compactCount(it) },
    )
    if (values.isEmpty()) return
    Row(
        modifier = Modifier.padding(top = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        values.forEach { (icon, count) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = .68f))
                Text(count, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = .68f))
            }
        }
    }
}

@Composable
private fun PrimaryPlayButton(
    isLoading: Boolean,
    hasStream: Boolean,
    focusRequester: FocusRequester,
    onPlay: () -> Unit,
    onRetry: () -> Unit,
) {
    Button(
        modifier = Modifier.width(116.dp).focusRequester(focusRequester),
        onClick = {
            when {
                isLoading -> Unit
                hasStream -> onPlay()
                else -> onRetry()
            }
        },
        colors = ButtonDefaults.colors(
            containerColor = MaterialTheme.colorScheme.onBackground,
            contentColor = MaterialTheme.colorScheme.background,
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            focusedContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        DetailsActionContent(
            if (!isLoading && !hasStream) Icons.Default.Refresh else Icons.Default.PlayArrow,
            when {
                isLoading -> "Preparing"
                hasStream -> "Play"
                else -> "Try again"
            },
            FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DetailsActionContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    weight: FontWeight = FontWeight.Medium,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(7.dp))
        Text(label, fontWeight = weight, maxLines = 1)
    }
}

private fun detailsMetadata(video: Video, stream: StreamDetails?): String = buildString {
    stream?.viewCount?.takeIf { it > 0L }?.let { append(compactCount(it)).append(" views") }
    video.relativeUploadDate().takeIf(String::isNotBlank)?.let { relativeDate ->
        if (isNotEmpty()) append("  •  ")
        append(relativeDate)
    }
    if (video.durationSeconds > 0L) {
        if (isNotEmpty()) append("  •  ")
        append(formatDetailsDuration(video.durationSeconds))
    }
    if (video.isLive) {
        if (isNotEmpty()) append("  •  ")
        append("LIVE")
    }
}

private fun compactCount(value: Long): String = when {
    value >= 1_000_000_000L -> "%.1fB".format(value / 1_000_000_000.0)
    value >= 1_000_000L -> "%.1fM".format(value / 1_000_000.0)
    value >= 1_000L -> "%.1fK".format(value / 1_000.0)
    else -> value.toString()
}

private fun formatDetailsDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "%d:%02d".format(minutes, remainingSeconds)
}
