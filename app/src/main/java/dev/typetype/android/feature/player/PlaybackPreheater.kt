package dev.typetype.android.feature.player

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.domain.stream.SabrPlaybackRepository
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamRepository
import dev.typetype.android.domain.stream.sabrPlaybackTarget
import dev.typetype.android.domain.usersettings.UserSettings
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackPreheater @Inject constructor(
    @ApplicationContext context: Context,
    private val streams: StreamRepository,
    private val sabr: SabrPlaybackRepository,
    private val preloads: SabrPlaybackPreloadStore,
    private val userSettingsRepository: UserSettingsRepository,
) {
    private val codecSupport: PlaybackCodecSupport = DevicePlaybackCodecSupport(context)

    suspend fun preheat(videoUrl: String) {
        val settings = userSettingsRepository.current().getOrNull() ?: return
        preheat(videoUrl, settings, codecSupport, { true })
    }

    internal suspend fun preheat(
        videoUrl: String,
        settings: UserSettings,
        codecSupport: PlaybackCodecSupport,
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
        if (reservation.owner) reservation.result.complete(sabr.prewarm(target, 0L))
    }
}

private fun Stream.isPreheatable(): Boolean =
    !isLive && !isPostLive && !isLiveContent && !requiresMembership
