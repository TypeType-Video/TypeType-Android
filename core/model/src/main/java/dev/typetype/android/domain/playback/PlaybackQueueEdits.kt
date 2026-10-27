package dev.typetype.android.domain.playback

fun PlaybackQueueState.removeEntry(index: Int): PlaybackQueueState? {
    if (!isActive || index !in entries.indices || index == currentIndex) return null
    val currentUrl = requireNotNull(current).videoUrl
    val updatedEntries = entries.toMutableList().apply { removeAt(index) }
    return copy(
        entries = updatedEntries,
        currentIndex = updatedEntries.indexOfFirst { it.videoUrl == currentUrl },
        isPreparingNext = false,
        failedVideoUrl = null,
    )
}

fun PlaybackQueueState.moveEntryNext(index: Int): PlaybackQueueState? {
    if (!isActive || index !in entries.indices || index == currentIndex + 1) return null
    if (index == currentIndex) return null
    val currentUrl = requireNotNull(current).videoUrl
    val movedEntry = entries[index]
    val updatedEntries = entries.toMutableList().apply {
        removeAt(index)
        add(indexOfFirst { it.videoUrl == currentUrl } + 1, movedEntry)
    }
    return copy(
        entries = updatedEntries,
        currentIndex = updatedEntries.indexOfFirst { it.videoUrl == currentUrl },
        isPreparingNext = false,
        failedVideoUrl = null,
    )
}

fun PlaybackQueueState.addEntry(
    entry: PlaybackQueueEntry,
    playNext: Boolean,
): PlaybackQueueState? {
    if (!isActive || entry.videoUrl.isBlank() || entries.any { it.videoUrl == entry.videoUrl }) {
        return null
    }
    val insertionIndex = if (playNext) currentIndex + 1 else entries.size
    val updatedEntries = entries.toMutableList().apply { add(insertionIndex, entry) }
    return copy(
        entries = updatedEntries,
        isPreparingNext = false,
        failedVideoUrl = null,
    )
}

fun PlaybackQueueState.enqueueEntry(
    entry: PlaybackQueueEntry,
    playNext: Boolean,
): PlaybackQueueMutation {
    val existingIndex = entries.indexOfFirst { it.videoUrl == entry.videoUrl }
    if (existingIndex == currentIndex) {
        return PlaybackQueueMutation(null, PlaybackQueueMutationResult.AlreadyPlaying)
    }
    if (existingIndex >= 0) {
        if (!playNext || existingIndex == currentIndex + 1) {
            return PlaybackQueueMutation(null, PlaybackQueueMutationResult.AlreadyQueued)
        }
        return PlaybackQueueMutation(
            state = moveEntryNext(existingIndex),
            result = PlaybackQueueMutationResult.Moved,
        )
    }
    return PlaybackQueueMutation(
        state = addEntry(entry, playNext),
        result = PlaybackQueueMutationResult.Added,
    )
}

fun PlaybackQueueState.shuffleUpcoming(): PlaybackQueueState? {
    val firstUpcoming = currentIndex + 1
    if (!isActive || entries.size - firstUpcoming < 2) return null
    return copy(
        entries = entries.take(firstUpcoming) + entries.drop(firstUpcoming).shuffled(),
        isPreparingNext = false,
        failedVideoUrl = null,
    )
}
