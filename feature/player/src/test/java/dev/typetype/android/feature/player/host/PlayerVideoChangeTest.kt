package dev.typetype.android.feature.player.host

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerVideoChangeTest {
    @Test
    fun `asking for another video stops the one playing`() {
        assertTrue(
            leavesPlayingVideo(
                requestedVideoUrl = "https://www.youtube.com/watch?v=R6MlUcmOul8",
                playingMediaId = "https://www.youtube.com/watch?v=eRsGyueVLvQ",
            ),
        )
    }

    @Test
    fun `the same video from another link keeps playing`() {
        assertFalse(
            leavesPlayingVideo(
                requestedVideoUrl = "https://youtu.be/eRsGyueVLvQ?t=42",
                playingMediaId = "https://www.youtube.com/watch?v=eRsGyueVLvQ",
            ),
        )
    }

    @Test
    fun `closing the player or an empty player is left alone`() {
        assertFalse(leavesPlayingVideo(null, "https://www.youtube.com/watch?v=eRsGyueVLvQ"))
        assertFalse(leavesPlayingVideo("https://www.youtube.com/watch?v=eRsGyueVLvQ", null))
    }
}
