package dev.typetype.android.domain.playback

enum class PlaybackSleepTimerMode {
    Off,
    Timed,
    EndOfVideo,
}

data class PlaybackSleepTimerState(
    val mode: PlaybackSleepTimerMode = PlaybackSleepTimerMode.Off,
    val durationMillis: Long = 0L,
    val remainingMillis: Long = 0L,
) {
    val isActive: Boolean
        get() = mode != PlaybackSleepTimerMode.Off
}
