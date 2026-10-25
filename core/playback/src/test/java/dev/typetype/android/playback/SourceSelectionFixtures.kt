package dev.typetype.android.playback

import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamAudioSource
import dev.typetype.android.domain.stream.StreamPlaybackContract
import dev.typetype.android.domain.stream.StreamVideoSource
import dev.typetype.android.playback.DecoderSupport
import dev.typetype.android.playback.PlaybackCodecSupport

internal object SourceSelectionFixtures {
    fun video(url: String, height: Int, codec: String, itag: Int = 0) = StreamVideoSource(
        url = url,
        mimeType = "video/mp4",
        codec = codec,
        resolution = "${height}p",
        width = height * 16 / 9,
        height = height,
        fps = 30,
        bitrate = height * 1_000,
        isVideoOnly = true,
        itag = itag,
    )

    fun audio(url: String, locale: String) = StreamAudioSource(
        url = url,
        mimeType = "audio/mp4",
        codec = "mp4a.40.2",
        bitrate = 128_000,
        quality = "medium",
        audioTrackId = url,
        audioTrackName = locale,
        audioLocale = locale,
        isOriginal = false,
    )

    fun providerStream(
        videos: List<StreamVideoSource>,
        audios: List<StreamAudioSource>,
    ) = Stream(
        playbackContract = StreamPlaybackContract.ProviderMedia,
        id = "video",
        title = "Video",
        uploaderName = "Channel",
        uploaderAvatarUrl = "avatar",
        uploaderUrl = "channel",
        uploaderSubscriberCount = 1,
        uploaderVerified = false,
        thumbnailUrl = "thumb",
        description = "",
        durationSeconds = 60,
        viewCount = 1,
        likeCount = 1,
        dislikeCount = 0,
        uploadedAtMillis = 1,
        hlsUrl = null,
        dashMpdUrl = null,
        progressiveUrl = null,
        serverDashManifestUrl = null,
        serverHlsManifestUrl = null,
        videoOnlyStreams = videos,
        audioStreams = audios,
        startPositionMillis = 0,
    )

    class FakeCodecSupport(
        private val video: Map<String, DecoderSupport> = emptyMap(),
        private val audio: Map<String, DecoderSupport> = emptyMap(),
    ) : PlaybackCodecSupport {
        override fun video(source: StreamVideoSource): DecoderSupport =
            video[source.url] ?: DecoderSupport.Unsupported

        override fun audio(source: StreamAudioSource): DecoderSupport =
            audio[source.url] ?: DecoderSupport.Unsupported
    }
}
