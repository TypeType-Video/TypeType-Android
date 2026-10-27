package dev.typetype.android.playback

import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamAudioSource
import dev.typetype.android.domain.stream.StreamPlaybackContract
import dev.typetype.android.domain.stream.StreamRequestScope
import dev.typetype.android.domain.stream.StreamVideoSource
import dev.typetype.android.playback.DecoderSupport
import dev.typetype.android.playback.PlaybackCodecSupport
import dev.typetype.android.playback.sabrRequestKey

internal object SabrFixtures {
    fun sabrStream() = Stream(
        playbackContract = StreamPlaybackContract.ServerSabr,
        id = "video",
        title = "Video",
        uploaderName = "Channel",
        uploaderAvatarUrl = "",
        uploaderUrl = "",
        uploaderSubscriberCount = 0,
        uploaderVerified = false,
        thumbnailUrl = "",
        description = "",
        durationSeconds = 60,
        viewCount = 0,
        likeCount = 0,
        dislikeCount = 0,
        uploadedAtMillis = 0,
        hlsUrl = "https://classic.example/live.m3u8",
        dashMpdUrl = "https://classic.example/video.mpd",
        progressiveUrl = "https://classic.example/video.mp4",
        serverDashManifestUrl = "https://instance.example/streams/manifest",
        serverHlsManifestUrl = "https://classic.example/live.m3u8",
        serverSabrManifestUrl = "https://instance.example/sabr/manifest/video",
        sabrVideoStreams = listOf(video()),
        sabrAudioStreams = listOf(audio()),
        requestScope = StreamRequestScope("server", "account", "https://instance.example/"),
        muxedVideoStreams = listOf(video().copy(url = "https://classic.example/video.mp4")),
        startPositionMillis = 0,
    )

    fun video(itag: Int = 137, height: Int = 1080) = StreamVideoSource(
        url = "https://instance.example/sabr/manifest/video",
        mimeType = "video/mp4",
        codec = "avc1.640028",
        resolution = "${height}p",
        width = height * 16 / 9,
        height = height,
        fps = 30,
        bitrate = 4_000_000,
        isVideoOnly = true,
        itag = itag,
    )

    fun audio() = StreamAudioSource(
        url = "https://instance.example/sabr/manifest/video",
        mimeType = "audio/mp4",
        codec = "mp4a.40.2",
        bitrate = 128_000,
        quality = "medium",
        audioTrackId = "en.0",
        audioTrackName = "English",
        audioLocale = "en",
        isOriginal = true,
        itag = 140,
    )

    fun preparedSession(videoItag: Int = 137) = SabrPlaybackSession(
        sessionId = "session",
        manifestUrl = "https://instance.example/api/sabr/playback/session/manifest",
        generation = 0,
        videoItag = videoItag,
        audioItag = 140,
        audioTrackId = "en.0",
    )

    fun binding(videoItag: Int = 137) = SabrPlaybackBinding(
        sessionId = "session",
        generation = 0L,
        videoItag = videoItag,
        audioItag = 140,
        audioTrackId = "en.0",
    )

    fun target(videoItag: Int = 137) = SabrPlaybackTarget(
        videoId = "video",
        requestScope = StreamRequestScope("server", "account", "https://instance.example/"),
        videoItag = videoItag,
        audioItag = 140,
        audioTrackId = "en.0",
    )

    fun requestKey(stream: Stream): String = requireNotNull(
        stream.sabrRequestKey(
            selectedQuality = "1080p",
            selectedAudioKey = "en.0",
            defaultAudioLanguage = "en",
            preferOriginalLanguage = true,
            codecSupport = SupportedCodecs,
        ),
    )

    object SupportedCodecs : PlaybackCodecSupport {
        override fun video(source: StreamVideoSource) = DecoderSupport.Hardware
        override fun audio(source: StreamAudioSource) = DecoderSupport.Hardware
    }
}
