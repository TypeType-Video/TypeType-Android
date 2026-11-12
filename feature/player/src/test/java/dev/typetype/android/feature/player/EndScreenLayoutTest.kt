package dev.typetype.android.feature.player

import dev.typetype.android.feature.player.state.ResizeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class EndScreenLayoutTest {
    private val wide = 16f / 9f

    @Test
    fun `a fitted video fills a container of the same ratio`() {
        val frame = endScreenFrame(1600f, 900f, wide, ResizeMode.Fit)

        assertEquals(EndScreenFrame(0f, 0f, 1600f, 900f), frame)
    }

    @Test
    fun `a fitted video is centred between side bars on a wider screen`() {
        val frame = endScreenFrame(2000f, 900f, wide, ResizeMode.Fit)

        assertEquals(1600f, frame.width, 0.01f)
        assertEquals(900f, frame.height, 0.01f)
        assertEquals(200f, frame.left, 0.01f)
        assertEquals(0f, frame.top, 0.01f)
    }

    @Test
    fun `a cropped video overflows the container on both sides`() {
        val frame = endScreenFrame(2000f, 900f, wide, ResizeMode.Crop)

        assertEquals(2000f, frame.width, 0.01f)
        assertEquals(1125f, frame.height, 0.01f)
        assertEquals(-112.5f, frame.top, 0.01f)
    }

    @Test
    fun `a stretched or unknown video uses the whole container`() {
        val container = EndScreenFrame(0f, 0f, 2000f, 900f)

        assertEquals(container, endScreenFrame(2000f, 900f, wide, ResizeMode.Stretch))
        assertEquals(container, endScreenFrame(2000f, 900f, 0f, ResizeMode.Fit))
    }
}
