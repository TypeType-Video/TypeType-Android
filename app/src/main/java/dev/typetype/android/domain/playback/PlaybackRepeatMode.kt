package dev.typetype.android.domain.playback

enum class PlaybackRepeatMode {
    Off,
    All,
    One;

    fun next(): PlaybackRepeatMode = when (this) {
        Off -> All
        All -> One
        One -> Off
    }

    companion object {
        fun fromStorage(value: String): PlaybackRepeatMode = entries.firstOrNull {
            it.name.equals(value, ignoreCase = true)
        } ?: Off
    }
}
