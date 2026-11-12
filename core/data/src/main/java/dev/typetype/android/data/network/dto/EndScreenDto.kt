package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class EndScreenItem(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val url: String = "",
    val thumbnailUrl: String = "",
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val left: Double = 0.0,
    val top: Double = 0.0,
    val width: Double = 0.0,
    val aspectRatio: Double = 0.0,
)
