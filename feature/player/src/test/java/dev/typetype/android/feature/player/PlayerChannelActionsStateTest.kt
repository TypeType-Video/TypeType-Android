package dev.typetype.android.feature.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerChannelActionsStateTest {
    @Test
    fun `subscription state compares canonical channel urls`() {
        val state = PlayerChannelActionsState(
            subscribedUrls = setOf("https://youtube.com/channel/UC123"),
            updatingUrl = "https://youtube.com/channel/UC456",
        )

        assertTrue(state.isSubscribed("http://youtube.com/channel/UC123/?source=player"))
        assertFalse(state.isSubscribed("https://youtube.com/channel/UC999"))
        assertTrue(state.isUpdating("https://youtube.com/channel/UC456/"))
    }
}
