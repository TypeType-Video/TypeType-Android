package dev.typetype.android.domain.stream

data class AudioOnlyStream(
    val url: String,
    val kind: AudioOnlyStreamKind,
    val mimeType: String,
    val codec: String?,
    val bitrate: Int?,
    val contentLength: Long?,
    val durationMillis: Long?,
)
