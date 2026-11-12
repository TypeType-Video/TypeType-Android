package dev.typetype.android.feature.player.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.common.Player
import dev.typetype.android.domain.stream.StreamSubtitleSource
import dev.typetype.android.domain.usersettings.CaptionStyles
import dev.typetype.android.feature.player.LoadSubtitleCues
import dev.typetype.android.feature.player.PlayerDanmakuState

@Composable
internal fun PlayerContentOverlays(
    player: Player,
    danmakuState: PlayerDanmakuState,
    isInPip: Boolean,
    controlsVisible: Boolean,
    subtitlesVisible: Boolean,
    externalSubtitle: StreamSubtitleSource?,
    loadSubtitleCues: LoadSubtitleCues,
    captionStyles: CaptionStyles,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        DanmakuOverlay(
            player = player,
            state = danmakuState,
            visible = !isInPip,
            modifier = Modifier.fillMaxSize(),
        )
        PlayerSubtitleOverlay(
            player = player,
            controlsVisible = controlsVisible && !isInPip,
            subtitlesVisible = subtitlesVisible,
            externalSource = externalSubtitle,
            loadExternalCues = loadSubtitleCues,
            captionStyles = captionStyles,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
