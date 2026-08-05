package dev.typetype.android.feature.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerContentLayoutTest {
    @Test
    fun `fullscreen always uses the viewport alone`() {
        assertEquals(
            PlayerContentLayoutMode.Fullscreen,
            playerContentLayoutMode(widthDp = 1280f, heightDp = 800f, isFullscreen = true),
        )
    }

    @Test
    fun `phone portrait keeps a single scrolling column`() {
        assertEquals(
            PlayerContentLayoutMode.SingleColumn,
            playerContentLayoutMode(widthDp = 412f, heightDp = 915f, isFullscreen = false),
        )
    }

    @Test
    fun `landscape phone does not masquerade as a tablet`() {
        assertEquals(
            PlayerContentLayoutMode.SingleColumn,
            playerContentLayoutMode(widthDp = 840f, heightDp = 393f, isFullscreen = false),
        )
    }

    @Test
    fun `wide tablet uses side by side panes`() {
        assertEquals(
            PlayerContentLayoutMode.TwoPane,
            playerContentLayoutMode(widthDp = 1280f, heightDp = 800f, isFullscreen = false),
        )
    }

    @Test
    fun `portrait tablet keeps a single scrolling column`() {
        assertEquals(
            PlayerContentLayoutMode.SingleColumn,
            playerContentLayoutMode(widthDp = 1067f, heightDp = 1440f, isFullscreen = false),
        )
    }
}
