package dev.typetype.player

data class PlaybackWindow(
    val generation: Long,
    val durationUs: Long,
    val startPositionUs: Long,
    val endOfStream: Boolean,
    val audio: PlaybackTrack,
    val video: PlaybackTrack?,
) {
    init {
        require(generation >= 0L)
        require(durationUs > 0L)
        require(startPositionUs >= 0L)
        require(audio.kind == PlaybackTrackKind.Audio)
        require(video == null || video.kind == PlaybackTrackKind.Video)
    }
}

data class PlaybackTrack(
    val kind: PlaybackTrackKind,
    val id: String,
    val mimeType: String,
    val initializationUrl: String,
    val segments: List<PlaybackSegment>,
) {
    init {
        require(id.isNotBlank())
        require(mimeType.isNotBlank())
        require(initializationUrl.isNotBlank())
        require(segments.isNotEmpty())
    }

    val endPositionUs: Long
        get() = segments.maxOf { it.endPositionUs }
}

data class PlaybackSegment(
    val url: String,
    val startPositionUs: Long,
    val durationUs: Long,
) {
    init {
        require(url.isNotBlank())
        require(startPositionUs >= 0L)
        require(durationUs > 0L)
    }

    val endPositionUs: Long
        get() = Math.addExact(startPositionUs, durationUs)
}

data class PlaybackBufferedRange(
    val trackId: String,
    val startPositionUs: Long,
    val endPositionUs: Long,
) {
    init {
        require(trackId.isNotBlank())
        require(startPositionUs >= 0L)
        require(endPositionUs >= startPositionUs)
    }
}

enum class PlaybackTrackKind {
    Audio,
    Video,
}
