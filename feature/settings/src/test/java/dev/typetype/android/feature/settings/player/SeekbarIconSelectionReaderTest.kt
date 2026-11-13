package dev.typetype.android.feature.settings.player

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeekbarIconSelectionReaderTest {
    @Test
    fun `an image under the limit is read whole`() {
        val bytes = ByteArray(1024) { it.toByte() }

        assertArrayEquals(bytes, readSeekbarIcon(ByteArrayInputStream(bytes), maxBytes = 2048))
    }

    @Test
    fun `an image over the limit is refused`() {
        assertNull(readSeekbarIcon(ByteArrayInputStream(ByteArray(4096)), maxBytes = 2048))
    }
}
