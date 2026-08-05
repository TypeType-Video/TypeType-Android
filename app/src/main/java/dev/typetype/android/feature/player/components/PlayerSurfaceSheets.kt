package dev.typetype.android.feature.player.components

import androidx.compose.runtime.Composable
import androidx.media3.session.MediaController
import dev.typetype.android.domain.stream.Chapter
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.feature.player.PlaybackCodecSupport
import dev.typetype.android.feature.player.PlayerDanmakuAction
import dev.typetype.android.feature.player.PlayerDanmakuState
import dev.typetype.android.feature.player.state.ResizeMode

internal data class PlayerOptionsState(
    val selectedCodec: String,
    val selectedQuality: String,
    val selectedAudioKey: String?,
    val selectedSubtitleKey: String?,
    val selectedSpeed: Float,
    val resizeMode: ResizeMode,
    val audioOnlyEnabled: Boolean,
    val audioOnlyChanging: Boolean,
    val showAudioOnly: Boolean,
    val danmakuState: PlayerDanmakuState,
)

internal class PlayerOptionsActions(
    val onSelectCodec: (String) -> Unit,
    val onSelectQuality: (String) -> Unit,
    val onSelectAudio: (String?) -> Unit,
    val onSelectSubtitle: (String?) -> Unit,
    val onSelectSpeed: (Float) -> Unit,
    val onSelectResizeMode: (ResizeMode) -> Unit,
    val onAudioOnlyChange: (Boolean) -> Unit,
    val onDanmakuAction: (PlayerDanmakuAction) -> Unit,
)

@Composable
internal fun PlayerSurfaceSheets(
    player: MediaController,
    stream: Stream,
    codecSupport: PlaybackCodecSupport,
    optionsVisible: Boolean,
    chaptersVisible: Boolean,
    isInPip: Boolean,
    chapters: List<Chapter>,
    options: PlayerOptionsState,
    actions: PlayerOptionsActions,
    onDismissOptions: () -> Unit,
    onDismissChapters: () -> Unit,
) {
    if (optionsVisible && !isInPip) {
        PlaybackOptionsSheet(
            player = player,
            stream = stream,
            selectedCodec = options.selectedCodec,
            selectedQuality = options.selectedQuality,
            selectedAudioKey = options.selectedAudioKey,
            selectedSubtitleKey = options.selectedSubtitleKey,
            selectedSpeed = options.selectedSpeed,
            codecSupport = codecSupport,
            resizeMode = options.resizeMode,
            audioOnlyEnabled = options.audioOnlyEnabled,
            audioOnlyChanging = options.audioOnlyChanging,
            showAudioOnly = options.showAudioOnly,
            danmakuState = options.danmakuState,
            onSelectCodec = actions.onSelectCodec,
            onSelectQuality = actions.onSelectQuality,
            onSelectAudio = actions.onSelectAudio,
            onSelectSubtitle = actions.onSelectSubtitle,
            onSelectSpeed = actions.onSelectSpeed,
            onSelectResizeMode = actions.onSelectResizeMode,
            onAudioOnlyChange = actions.onAudioOnlyChange,
            onDanmakuAction = actions.onDanmakuAction,
            onDismiss = onDismissOptions,
        )
    }

    if (chaptersVisible && chapters.isNotEmpty() && !isInPip) {
        ChaptersSheet(
            chapters = chapters,
            currentPositionMs = player.currentPosition,
            onChapterClick = { chapter ->
                player.seekTo(chapter.startMs)
                onDismissChapters()
            },
            onDismiss = onDismissChapters,
        )
    }
}
