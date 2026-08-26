package dev.typetype.android.data.network.dto

import dev.typetype.android.domain.imports.PipePipeRestoreSummary
import kotlinx.serialization.Serializable

@Serializable
data class PipePipeRestoreSummaryDto(
    val history: Int,
    val subscriptions: Int,
    val playlists: Int,
    val playlistVideos: Int,
    val progress: Int,
    val searchHistory: Int,
    val timeMode: String,
    val historyMinWatchedAt: Long? = null,
    val historyMaxWatchedAt: Long? = null,
)

fun PipePipeRestoreSummaryDto.toDomain() = PipePipeRestoreSummary(
    history = history,
    subscriptions = subscriptions,
    playlists = playlists,
    playlistVideos = playlistVideos,
    progress = progress,
    searchHistory = searchHistory,
    historyMinWatchedAt = historyMinWatchedAt,
    historyMaxWatchedAt = historyMaxWatchedAt,
)
@Serializable
data class PortabilityCapabilityDto(
    val category: String,
    val directions: List<String> = emptyList(),
    val fidelity: String = "complete",
)

@Serializable
data class PortabilityFormatDto(
    val format: String,
    val adapterVersion: Int,
    val capabilities: List<PortabilityCapabilityDto> = emptyList(),
    val defaultExtension: String,
    val contentType: String,
)

@Serializable
data class PortabilityExportRequestDto(
    val format: String,
    val categories: List<String>,
)

@Serializable
data class PortabilityApplyRequestDto(
    val categories: List<String>,
    val duplicatePolicy: String,
)

@Serializable
data class PortabilityIssueDto(
    val category: String? = null,
    val code: String,
    val message: String,
    val count: Int = 1,
)

@Serializable
data class PortabilityPreviewDto(
    val detection: PortabilityDetectionDto? = null,
    val counts: Map<String, Int> = emptyMap(),
    val duplicates: Int = 0,
    val issues: List<PortabilityIssueDto> = emptyList(),
)

@Serializable
data class PortabilityDetectionDto(
    val format: String,
    val formatVersion: String? = null,
    val adapterVersion: Int,
    val confidence: Double,
    val evidence: String,
)

@Serializable
data class PortabilityProgressDto(
    val phase: String? = null,
    val unit: String? = null,
    val processed: Int = 0,
    val total: Int? = null,
)

@Serializable
data class PortabilityJobDto(
    val id: String,
    val kind: String,
    val state: String,
    val createdAt: Long,
    val updatedAt: Long,
    val requestId: String? = null,
    val preview: PortabilityPreviewDto? = null,
    val result: Map<String, Int>? = null,
    val progress: PortabilityProgressDto? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
)
