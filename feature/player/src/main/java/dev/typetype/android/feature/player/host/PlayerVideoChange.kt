package dev.typetype.android.feature.player.host

import dev.typetype.android.domain.navigation.canonicalVideoIdentity

// The shared player keeps the previous video running until the next one is loaded, a few seconds later; asking for
// another video stops its sound at once, as the web player does when it leaves a page.
internal fun leavesPlayingVideo(requestedVideoUrl: String?, playingMediaId: String?): Boolean {
    if (requestedVideoUrl.isNullOrBlank() || playingMediaId.isNullOrBlank()) return false
    return canonicalVideoIdentity(requestedVideoUrl) != canonicalVideoIdentity(playingMediaId)
}
