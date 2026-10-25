package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class YoutubeTakeoutPreviewDto(
    val counts: YoutubeTakeoutCategoryCountsDto,
    val dedup: YoutubeTakeoutCategoryCountsDto,
    val warnings: List<String>,
    val errors: List<String>,
    val issues: List<YoutubeTakeoutIssueDto> = emptyList(),
    val issueSummary: YoutubeTakeoutIssueSummaryDto,
)

@Serializable
data class YoutubeTakeoutCategoryCountsDto(
    val subscriptions: Int,
    val playlists: Int,
    val playlistItems: Int,
    val favorites: Int = 0,
    val watchLater: Int = 0,
    val history: Int = 0,
)

@Serializable
data class YoutubeTakeoutIssueDto(
    val code: String,
    val severity: String,
    val stage: String,
    val message: String,
    val count: Int = 1,
)

@Serializable
data class YoutubeTakeoutIssueSummaryDto(
    val total: Int,
    val warnings: Int,
    val errors: Int,
    val byCode: Map<String, Int> = emptyMap(),
)
