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
