package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class YoutubeTakeoutJobStatusDto(
    val jobId: String,
    val status: String,
    val phase: String,
    val progress: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val expiresAt: Long,
    val error: String? = null,
)
