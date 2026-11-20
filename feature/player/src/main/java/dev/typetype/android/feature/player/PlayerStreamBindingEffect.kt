package dev.typetype.android.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.media3.session.MediaController
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.usersettings.UserSettings
import dev.typetype.android.playback.PlaybackCodecSupport
import dev.typetype.android.playback.PrepareSabrPlayback

// Binds the stream to the media controller again whenever what it plays changes: another stream, a new bind or codec
// fallback generation, or a quality, audio or codec choice.
@Composable
internal fun PlayerStreamBindingEffect(
    controller: MediaController?,
    stream: Stream,
    videoUrl: String,
    resumeAtMillis: Long,
    selections: PlayerPlaybackSelectionState,
    userSettings: UserSettings,
    bindGenerations: Pair<Long, Long>,
    initialPlayWhenReady: Boolean,
    codecSupport: PlaybackCodecSupport,
    prepareSabrPlayback: PrepareSabrPlayback,
) {
    LaunchedEffect(
        stream.id,
        stream.requestScope,
        controller,
        bindGenerations,
        selections.selectedCodec,
        selections.selectedQuality,
        selections.selectedAudioKey,
        userSettings.defaultAudioLanguage,
        userSettings.defaultQuality,
        userSettings.preferOriginalLanguage,
        initialPlayWhenReady,
    ) {
        controller?.let { ctrl ->
            bindStreamToController(
                controller = ctrl,
                stream = stream,
                videoUrl = videoUrl,
                startMillis = resumeAtMillis,
                selectedQuality = selections.selectedQuality,
                selectedAudioKey = selections.selectedAudioKey,
                selectedSubtitleKey = selections.selectedSubtitleKey,
                defaultAudioLanguage = userSettings.defaultAudioLanguage,
                automaticQualityCap = userSettings.defaultQuality,
                preferOriginalLanguage = userSettings.preferOriginalLanguage,
                initialPlayWhenReady = initialPlayWhenReady,
                codecSupport = codecSupport,
                prepareSabrPlayback = prepareSabrPlayback,
                selectedCodec = selections.selectedCodec,
            )
        }
    }
}
