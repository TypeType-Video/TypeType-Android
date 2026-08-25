package dev.typetype.android.feature.player.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.typetype.android.core.ui.branding.rememberVideoBranding
import dev.typetype.android.domain.stream.Stream

@Composable
internal fun DeArrowAudioOnlyPoster(
    stream: Stream,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val branding = rememberVideoBranding(
        sourceUrl = stream.id,
        title = stream.title,
        thumbnailUrl = stream.thumbnailUrl,
        durationSeconds = stream.durationSeconds,
    )
    AudioOnlyPoster(
        thumbnailUrl = branding.thumbnailUrl,
        title = branding.title,
        isPlaying = isPlaying,
        modifier = modifier,
    )
}
