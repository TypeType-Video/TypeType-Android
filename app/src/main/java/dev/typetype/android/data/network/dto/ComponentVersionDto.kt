package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ComponentVersionDto(
    val service: String,
    val version: String,
    val revision: String,
    val shortRevision: String,
    val buildTime: String,
)
