package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class TypeTypeRestoreSummaryDto(
    val restored: Map<String, Int> = emptyMap(),
)
