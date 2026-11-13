package dev.typetype.android.domain.progress

import kotlinx.coroutines.flow.StateFlow

interface WatchProgressSource {
    val positions: StateFlow<Map<String, Long>>
    suspend fun load(videoUrls: Collection<String>)
}
