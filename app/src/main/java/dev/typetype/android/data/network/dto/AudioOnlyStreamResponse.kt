package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class AudioOnlyStreamResponse(
    val src: String,
    val kind: String,
    val mimeType: String,
    val codec: String? = null,
    val bitrate: Int? = null,
    val contentLength: Long? = null,
    val duration: Long? = null,
)
