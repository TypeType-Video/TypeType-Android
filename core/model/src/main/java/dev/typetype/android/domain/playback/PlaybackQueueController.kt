package dev.typetype.android.domain.playback

import kotlinx.coroutines.flow.StateFlow

interface PlaybackQueueController {
    val state: StateFlow<PlaybackQueueState>
    fun start(title: String, entries: List<PlaybackQueueEntry>, shuffle: Boolean)
    fun restore(snapshot: PlaybackQueueSnapshot)
    fun clear()

    fun enqueue(entry: PlaybackQueueEntry, playNext: Boolean): PlaybackQueueMutationResult
}
