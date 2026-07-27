package dev.typetype.android.services

import android.content.Context
import androidx.media3.common.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.domain.stream.SabrPlaybackRepository
import dev.typetype.android.domain.stream.StreamRepository
import dev.typetype.android.domain.stream.sabrPlaybackTarget
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import dev.typetype.android.feature.player.DevicePlaybackCodecSupport
import dev.typetype.android.feature.player.buildResolvedMediaItem
import dev.typetype.android.feature.player.initialAudioKey
import dev.typetype.android.feature.player.initialQuality
import dev.typetype.android.feature.player.pickPlayableSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class PlaybackQueueItemResolver @Inject constructor(
    @ApplicationContext context: Context,
    private val streamRepository: StreamRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val sabrPlaybackRepository: SabrPlaybackRepository,
) {
    private val codecSupport = DevicePlaybackCodecSupport(context)

    suspend fun resolve(videoUrl: String): Result<MediaItem> = captureQueueResult {
        val settings = userSettingsRepository.current().getOrThrow()
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
                automaticQualityCap = settings.defaultQuality,
                preferOriginalLanguage = settings.preferOriginalLanguage,
                codecSupport = codecSupport,
                prepareSabrPlayback = { loaded, selection, _ ->
                    sabrPlaybackRepository.prepare(loaded.sabrPlaybackTarget(selection)).getOrThrow()
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
