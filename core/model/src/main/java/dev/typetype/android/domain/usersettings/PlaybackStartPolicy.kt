package dev.typetype.android.domain.usersettings

fun interface PlaybackStartPolicy {
    fun playWhenOpened(): Boolean

    companion object {
        val Always = PlaybackStartPolicy { true }
    }
}
