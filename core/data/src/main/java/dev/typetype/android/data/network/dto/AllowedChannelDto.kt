package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class AllowedChannelDto(
    val url: String,
    val name: String? = null,
    val thumbnailUrl: String? = null,
    val global: Boolean? = null,
)
