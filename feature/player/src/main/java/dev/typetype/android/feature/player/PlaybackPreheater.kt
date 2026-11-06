package dev.typetype.android.feature.player

import dev.typetype.android.domain.preferences.RECOMMENDED_CODEC_KEY
import dev.typetype.android.domain.stream.SabrPlaybackRepository
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamRepository
import dev.typetype.android.domain.stream.sabrPlaybackTarget
import dev.typetype.android.domain.usersettings.UserSettings
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import dev.typetype.android.playback.DevicePlaybackCodecSupport
import dev.typetype.android.playback.PlaybackCodecSupport
import dev.typetype.android.playback.effectiveQuality
import dev.typetype.android.playback.initialAudioKey
import dev.typetype.android.playback.initialQuality
import dev.typetype.android.playback.sabrSelection
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class PlaybackPreheater @Inject constructor(
    private val streams: StreamRepository,
    private val sabr: SabrPlaybackRepository,
    private val preloads: SabrPlaybackPreloadStore,
    private val userSettingsRepository: UserSettingsRepository,
    private val codecSupport: DevicePlaybackCodecSupport,
) {

    suspend fun prefetchStream(videoUrl: String) {
        streams.prefetchPlaybackStream(videoUrl)
    }

    suspend fun preheat(videoUrl: String, beforeCreate: suspend () -> Unit) {
        val settings = userSettingsRepository.current().getOrNull() ?: return
        preheat(videoUrl, settings, codecSupport, beforeCreate) { true }
    }

    suspend fun preheat(
        videoUrl: String,
        settings: UserSettings,
        codecSupport: PlaybackCodecSupport,
        beforeCreate: suspend () -> Unit = {},
        canPrepare: () -> Boolean,
    ) {
        val stream = streams.prefetchPlaybackStream(videoUrl).getOrNull() ?: return
        if (!stream.isPreheatable()) return
        if (!canPrepare()) return
        val selection = stream.sabrSelection(
            selectedQuality = stream.initialQuality().effectiveQuality(settings.defaultQuality),
            selectedAudioKey = stream.initialAudioKey(
                settings.defaultAudioLanguage,
                settings.preferOriginalLanguage,
            ),
            defaultAudioLanguage = settings.defaultAudioLanguage,
            preferOriginalLanguage = settings.preferOriginalLanguage,
            codecSupport = codecSupport,
            selectedCodec = RECOMMENDED_CODEC_KEY,
        ) ?: return
        val target = stream.sabrPlaybackTarget(selection)
        val reservation = preloads.reserve(target)
        if (!reservation.owner) return
        try {
            beforeCreate()
            reservation.result.complete(sabr.prewarm(target, 0L))
        } catch (cancelled: CancellationException) {
            preloads.discard(target, reservation.result)
            reservation.result.complete(Result.failure(cancelled))
            throw cancelled
        }
    }
}

private fun Stream.isPreheatable(): Boolean =
    !isLive && !isPostLive && !isLiveContent && !requiresMembership
