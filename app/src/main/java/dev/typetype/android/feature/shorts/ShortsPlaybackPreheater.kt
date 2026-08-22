package dev.typetype.android.feature.shorts

import dev.typetype.android.domain.stream.SabrPlaybackRepository
import dev.typetype.android.domain.stream.StreamRepository
import dev.typetype.android.domain.stream.sabrPlaybackTarget
import dev.typetype.android.domain.usersettings.UserSettings
import dev.typetype.android.feature.player.PlaybackCodecSupport
import dev.typetype.android.feature.player.RECOMMENDED_CODEC_KEY
import dev.typetype.android.feature.player.effectiveQuality
import dev.typetype.android.feature.player.initialAudioKey
import dev.typetype.android.feature.player.initialQuality
import dev.typetype.android.feature.player.SabrPlaybackPreloadStore
import dev.typetype.android.feature.player.sabrSelection
import javax.inject.Inject

class ShortsPlaybackPreheater @Inject constructor(
    private val streams: StreamRepository,
    private val sabr: SabrPlaybackRepository,
    private val preloads: SabrPlaybackPreloadStore,
) {
    internal suspend fun preheat(
        videoUrl: String,
        settings: UserSettings,
        codecSupport: PlaybackCodecSupport,
        prepareSession: () -> Boolean,
    ) {
        val stream = streams.prefetchPlaybackStream(videoUrl).getOrNull() ?: return
        if (!prepareSession()) return
        if (stream.isLive || stream.isLiveContent) return
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
        if (reservation.owner) reservation.result.complete(sabr.prepare(target, 0L))
    }
}
