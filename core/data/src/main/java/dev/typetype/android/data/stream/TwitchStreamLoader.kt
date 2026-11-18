package dev.typetype.android.data.stream

import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamPlaybackContract
import dev.typetype.android.domain.twitch.TwitchRepository
import dev.typetype.android.domain.twitch.TwitchTarget
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

// Twitch plays from TypeType-Live's signed playlists (lives and past broadcasts) or a clip's MP4 file; nothing goes
// through the server's stream extractor.
@Singleton
internal class TwitchStreamLoader @Inject constructor(
    private val twitch: TwitchRepository,
) {
    suspend fun load(target: TwitchTarget): Stream = when (target) {
        is TwitchTarget.Live -> {
            val playback = twitch.live(target.login).getOrThrow() ?: throw TwitchOfflineException()
            stream(playback.video, hlsUrl = playback.playlistUrl, live = true).copy(startOverUrl = playback.startOverUrl)
        }
        is TwitchTarget.Vod -> coroutineScope {
            val chapters = async { twitch.chapters(target.id).getOrNull().orEmpty().takeIf { it.size > 1 }.orEmpty() }
            val playback = twitch.vod(target.id).getOrThrow()
            // A recording in progress plays like a past broadcast from where it was opened, with no live target to chase.
            stream(playback.video, hlsUrl = playback.playlistUrl, live = false)
                .copy(chapters = chapters.await(), holdsStartPosition = playback.live)
        }
        is TwitchTarget.Clip -> {
            val clip = twitch.clip(target.slug).getOrThrow()
            val quality = clip.qualities.firstOrNull { it.height <= CLIP_HEIGHT } ?: clip.qualities.last()
            stream(clip.video, progressiveUrl = quality.url, live = false)
        }
    }

    private fun stream(video: Video, hlsUrl: String? = null, progressiveUrl: String? = null, live: Boolean) = Stream(
        playbackContract = StreamPlaybackContract.ProviderMedia,
        id = video.url,
        title = video.title,
        uploaderName = video.uploaderName,
        uploaderAvatarUrl = video.uploaderAvatarUrl,
        uploaderUrl = video.uploaderUrl,
        uploaderSubscriberCount = 0,
        uploaderVerified = false,
        thumbnailUrl = video.thumbnailUrl,
        description = "",
        durationSeconds = if (live) 0 else video.durationSeconds,
        viewCount = video.viewCount,
        likeCount = 0,
        dislikeCount = 0,
        uploadedAtMillis = video.uploadedAtMillis,
        hlsUrl = hlsUrl,
        dashMpdUrl = null,
        progressiveUrl = progressiveUrl,
        serverDashManifestUrl = null,
        serverHlsManifestUrl = null,
        startPositionMillis = 0,
        isLive = live,
        isLiveContent = live,
        liveTargetOffsetMillis = if (live) LOW_LATENCY_TARGET_MS else null,
    )

    private companion object {
        const val CLIP_HEIGHT = 1080
        // The web player's low latency start: 8 s back with a few complete 2 s segments buffered.
        const val LOW_LATENCY_TARGET_MS = 8_000L
    }
}

private class TwitchOfflineException : IllegalStateException("This Twitch channel is not live"), CodedFailure {
    override val failureCode: String = "twitch_offline"
    override val requestId: String? = null
    override val statusCode: Int = 404
}
