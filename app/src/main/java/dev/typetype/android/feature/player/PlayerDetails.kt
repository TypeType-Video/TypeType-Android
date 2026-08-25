package dev.typetype.android.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import dev.typetype.android.core.ui.branding.rememberVideoBranding
import dev.typetype.android.domain.playback.PlaybackQueueState
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.usersettings.UserSettings
import dev.typetype.android.feature.menu.rememberVideoMenuScope
import dev.typetype.android.feature.player.components.DescriptionSection
import dev.typetype.android.feature.player.components.AudioOnlyPlaybackState
import dev.typetype.android.feature.player.components.RelatedStreamsSection
import dev.typetype.android.feature.player.components.UploaderCard
import dev.typetype.android.feature.player.queue.PlaybackQueueControls
import dev.typetype.android.feature.player.sleep.PlaybackSleepTimerControls

@Composable
internal fun PlayerDetails(
    stream: Stream,
    videoUrl: String,
    player: Player?,
    userSettings: UserSettings,
    playbackQueue: PlaybackQueueState,
    isFavorited: Boolean,
    isInWatchLater: Boolean,
    isSubscribed: Boolean,
    subscriptionInFlight: Boolean,
    downloadInFlight: Boolean,
    audioOnlyState: AudioOnlyPlaybackState?,
    onAction: (PlayerAction) -> Unit,
    onShowComments: () -> Unit,
    onShowDownloads: () -> Unit,
    onPlayVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    onToggleSubscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val videoMenuScope = rememberVideoMenuScope(onOpenChannel = onOpenChannel)
    val branding = rememberVideoBranding(
        sourceUrl = videoUrl,
        title = stream.title,
        thumbnailUrl = stream.thumbnailUrl,
        durationSeconds = stream.durationSeconds,
    )
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DescriptionSection(
            title = branding.title,
            viewCount = stream.viewCount,
            likeCount = stream.likeCount,
            description = stream.description,
            onTimestampClick = { player?.seekTo(it) },
        )
        PlaybackQueueControls(playbackQueue)
        PlaybackSleepTimerControls()
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        PlayerInteractionRow(
            isFavorited = isFavorited,
            isInWatchLater = isInWatchLater,
            shareUrl = videoUrl,
            onToggleFavorite = { onAction(PlayerAction.OnToggleFavorite) },
            onToggleWatchLater = { onAction(PlayerAction.OnToggleWatchLater) },
            onAddToPlaylist = { onAction(PlayerAction.OnOpenPlaylistPicker) },
            onShowComments = onShowComments.takeUnless { userSettings.hideComments },
            onDownload = onShowDownloads,
            downloadInFlight = downloadInFlight,
            audioOnlyEnabled = audioOnlyState?.active == true,
            audioOnlyAvailable = audioOnlyState?.available == true,
            audioOnlyChanging = audioOnlyState?.changing == true,
            onToggleAudioOnly = {
                audioOnlyState?.setEnabled(audioOnlyState.active.not())
            },
        )
        UploaderCard(
            name = stream.uploaderName,
            avatarUrl = stream.uploaderAvatarUrl,
            subscriberCount = stream.uploaderSubscriberCount,
            verified = stream.uploaderVerified,
            isSubscribed = isSubscribed,
            subscriptionInFlight = subscriptionInFlight,
            onCardClick = { onOpenChannel(stream.uploaderUrl) },
            onSubscribeClick = onToggleSubscription,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        if (!userSettings.hideRelatedVideos) {
            RelatedStreamsSection(
                videos = stream.relatedStreams,
                onPlayVideo = onPlayVideo,
                menuScope = videoMenuScope,
                onOpenChannel = onOpenChannel,
                autoplayEnabled = userSettings.autoplay,
                onAutoplayChange = { onAction(PlayerAction.OnSetAutoplay(it)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
