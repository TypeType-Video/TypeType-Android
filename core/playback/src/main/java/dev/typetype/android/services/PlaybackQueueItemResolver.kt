package dev.typetype.android.services

import androidx.media3.common.MediaItem
import dev.typetype.android.data.network.PlaybackNetworkMonitor
import dev.typetype.android.domain.preferences.PreferencesRepository
import dev.typetype.android.domain.preferences.effectivePlaybackQualityCap
import dev.typetype.android.domain.stream.SabrPlaybackRepository
import dev.typetype.android.domain.stream.StreamRepository
import dev.typetype.android.domain.stream.sabrPlaybackTarget
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import dev.typetype.android.playback.DevicePlaybackCodecSupport
import dev.typetype.android.playback.buildResolvedMediaItem
import dev.typetype.android.playback.initialAudioKey
import dev.typetype.android.playback.initialQuality
import dev.typetype.android.playback.pickPlayableSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

@Singleton
class PlaybackQueueItemResolver @Inject constructor(
    private val streamRepository: StreamRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val preferencesRepository: PreferencesRepository,
    private val networkMonitor: PlaybackNetworkMonitor,
    private val sabrPlaybackRepository: SabrPlaybackRepository,
    private val codecSupport: DevicePlaybackCodecSupport,
) {

    suspend fun resolve(videoUrl: String): Result<MediaItem> = captureQueueResult {
        val settings = userSettingsRepository.current().getOrThrow()
        val preferences = preferencesRepository.observe().first()
        val automaticQualityCap = effectivePlaybackQualityCap(
            defaultQuality = preferences.playerDefaultQuality,
            mobileDataQuality = preferences.playerMobileDataQuality,
            isMetered = networkMonitor.snapshot().isMetered,
        )
        val stream = streamRepository.loadPlaybackStream(videoUrl).getOrThrow()
        val selectedAudioKey = stream.initialAudioKey(
            settings.defaultAudioLanguage,
            settings.preferOriginalLanguage,
        )
        val source = checkNotNull(
            pickPlayableSource(
                stream = stream,
                selectedQuality = stream.initialQuality(),
                selectedAudioKey = selectedAudioKey,
                defaultAudioLanguage = settings.defaultAudioLanguage,
                automaticQualityCap = automaticQualityCap,
                preferOriginalLanguage = settings.preferOriginalLanguage,
                codecSupport = codecSupport,
                prepareSabrPlayback = { loaded, selection, _ ->
                    sabrPlaybackRepository.prewarm(loaded.sabrPlaybackTarget(selection)).getOrThrow()
                },
            ),
        ) { "No playable source for queued video" }
        buildResolvedMediaItem(
            stream = stream,
            videoUrl = videoUrl,
            source = source,
            subtitles = stream.subtitles,
            startPositionMillis = 0L,
        )
    }
}

private suspend fun <T> captureQueueResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Throwable) {
    Result.failure(failure)
}
