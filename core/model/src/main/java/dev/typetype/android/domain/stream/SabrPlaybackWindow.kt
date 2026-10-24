package dev.typetype.android.domain.stream

data class SabrPlaybackWindowTrack(
    val itag: Int,
    val mimeType: String,
    val initializationUrl: String,
    val segments: List<SabrPlaybackWindowSegment>,
) {
    val endMs: Long
        get() = segments.maxOf { Math.addExact(it.startMs, it.durationMs) }
}

data class SabrPlaybackWindowSegment(
    val url: String,
    val startMs: Long,
    val durationMs: Long,
)
