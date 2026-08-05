package dev.typetype.android.feature.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Headphones
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
    audioOnlyEnabled: Boolean,
    audioOnlyChanging: Boolean,
    showAudioOnly: Boolean,
    showDanmaku: Boolean,
    danmakuEnabled: Boolean,
    danmakuSpeedLabel: String,
    danmakuSizeLabel: String,
    danmakuLoading: Boolean,
    danmakuLoadFailed: Boolean,
    onOpenCodec: () -> Unit,
    onOpenQuality: () -> Unit,
    onOpenCaptions: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenResize: () -> Unit,
    onAudioOnlyChange: (Boolean) -> Unit,
    onDanmakuChange: (Boolean) -> Unit,
    onOpenDanmakuSpeed: () -> Unit,
    onOpenDanmakuSize: () -> Unit,
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
        if (showAudioOnly) {
            item {
                PlaybackOptionsToggleRow(
                    icon = Icons.Filled.Headphones,
                    title = stringResource(R.string.player_audio_only),
                    checked = audioOnlyEnabled,
                    enabled = !audioOnlyChanging,
                    onCheckedChange = onAudioOnlyChange,
                )
            }
        }
        if (showDanmaku) {
            item {
                PlaybackOptionsToggleRow(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    title = stringResource(R.string.settings_player_danmaku_enabled),
                    checked = danmakuEnabled,
                    enabled = true,
                    onCheckedChange = onDanmakuChange,
                )
            }
            if (danmakuEnabled) {
                item {
                    PlaybackOptionsMenuRow(
                        icon = Icons.Filled.Speed,
                        title = stringResource(R.string.settings_player_danmaku_speed),
                        value = danmakuSpeedLabel,
                        onClick = onOpenDanmakuSpeed,
                    )
                }
                item {
                    PlaybackOptionsMenuRow(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        title = stringResource(R.string.settings_player_danmaku_size),
                        value = danmakuSizeLabel,
                        onClick = onOpenDanmakuSize,
                    )
                }
                when {
                    danmakuLoading -> item {
                        PlaybackOptionsHint(stringResource(R.string.player_danmaku_loading))
                    }
                    danmakuLoadFailed -> item {
                        PlaybackOptionsHint(stringResource(R.string.player_danmaku_unavailable))
                    }
                }
            }
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
