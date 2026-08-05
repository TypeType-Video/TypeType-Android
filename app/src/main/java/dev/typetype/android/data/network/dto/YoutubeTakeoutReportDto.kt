package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class YoutubeTakeoutReportDto(
    val subscriptions: YoutubeTakeoutImportStatsDto,
    val playlists: YoutubeTakeoutImportStatsDto,
    val playlistItems: YoutubeTakeoutImportStatsDto,
    val favorites: YoutubeTakeoutImportStatsDto,
    val watchLater: YoutubeTakeoutImportStatsDto,
    val history: YoutubeTakeoutImportStatsDto,
    val warnings: List<String>,
    val errors: List<String>,
    val issues: List<YoutubeTakeoutIssueDto> = emptyList(),
    val issueSummary: YoutubeTakeoutIssueSummaryDto,
    val finishedAt: Long,
)

@Serializable
data class YoutubeTakeoutImportStatsDto(
    val imported: Int,
    val skipped: Int,
    val failed: Int,
)
