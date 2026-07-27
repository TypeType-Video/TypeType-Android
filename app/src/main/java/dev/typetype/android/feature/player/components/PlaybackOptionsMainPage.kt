package dev.typetype.android.feature.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.typetype.android.R

@Composable
fun PlaybackOptionsMainPage(
    codecLabel: String,
    qualityLabel: String,
    captionsLabel: String,
    audioLabel: String,
    speedLabel: String,
    resizeLabel: String,
    onOpenCodec: () -> Unit,
    onOpenQuality: () -> Unit,
    onOpenCaptions: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenResize: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            PlaybackOptionsMenuRow(
                icon = Icons.Filled.Settings,
                title = stringResource(R.string.playback_options_codec),
                value = codecLabel,
                onClick = onOpenCodec,
            )
        }
        item {
            PlaybackOptionsMenuRow(
                icon = Icons.Filled.AspectRatio,
                title = stringResource(R.string.playback_options_quality),
                value = qualityLabel,
                onClick = onOpenQuality,
            )
        }
        item {
            PlaybackOptionsMenuRow(
                icon = Icons.Filled.ClosedCaption,
                title = stringResource(R.string.playback_options_captions),
                value = captionsLabel,
                onClick = onOpenCaptions,
            )
        }
        item {
            PlaybackOptionsMenuRow(
                icon = Icons.Filled.Audiotrack,
                title = stringResource(R.string.playback_options_audio),
                value = audioLabel,
                onClick = onOpenAudio,
            )
        }
        item {
            PlaybackOptionsMenuRow(
                icon = Icons.Filled.Speed,
                title = stringResource(R.string.playback_options_speed),
                value = speedLabel,
                onClick = onOpenSpeed,
            )
        }
        item {
            PlaybackOptionsMenuRow(
                icon = Icons.Filled.AspectRatio,
                title = stringResource(R.string.playback_options_image),
                value = resizeLabel,
                onClick = onOpenResize,
            )
        }
    }
}
