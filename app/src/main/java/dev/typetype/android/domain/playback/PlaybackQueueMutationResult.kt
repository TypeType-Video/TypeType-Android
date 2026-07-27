package dev.typetype.android.domain.playback

enum class PlaybackQueueMutationResult {
    Added,
    Moved,
    AlreadyQueued,
    AlreadyPlaying,
    NoActivePlayback,
}

data class PlaybackQueueMutation(
    val state: PlaybackQueueState?,
    val result: PlaybackQueueMutationResult,
)
