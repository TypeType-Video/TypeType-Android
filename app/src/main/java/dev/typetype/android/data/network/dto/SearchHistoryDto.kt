package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class SearchHistoryItemDto(
    val id: String = "",
    val term: String = "",
    val searchedAt: Long = 0L,
)

@Serializable
data class SearchHistoryEntryRequest(val term: String)
