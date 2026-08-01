package dev.typetype.android.feature.settings.profile

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class AvatarSelectionReaderTest {
    @Test
    fun returnsEveryByteWithinTheLimit() {
        val expected = ByteArray(128) { it.toByte() }

        val actual = readBoundedAvatar(
            input = ByteArrayInputStream(expected),
            maxBytes = expected.size,
        )

        assertArrayEquals(expected, actual)
    }

    @Test
    fun rejectsAFileOneByteOverTheLimit() {
        val oversized = ByteArray(129)

        assertThrows(IllegalStateException::class.java) {
            readBoundedAvatar(
                input = ByteArrayInputStream(oversized),
                maxBytes = 128,
            )
        }
    }

    @Test
    fun detectsSupportedFormatsFromTheirBytes() {
        assertEquals("image/jpeg", detectAvatarMediaType(bytes(0xff, 0xd8, 0xff)))
        assertEquals(
            "image/png",
            detectAvatarMediaType(bytes(0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)),
        )
        assertEquals("image/gif", detectAvatarMediaType("GIF87a".encodeToByteArray()))
        assertEquals("image/gif", detectAvatarMediaType("GIF89a".encodeToByteArray()))
        assertEquals(
            "image/webp",
            detectAvatarMediaType("RIFF0000WEBP".encodeToByteArray()),
        )
    }

    @Test
    fun rejectsUnsupportedBytesRegardlessOfThePickerMimeType() {
        assertNull(detectAvatarMediaType("not an image".encodeToByteArray()))
    }

    @Test
    fun pickerOffersOnlyServerSupportedFormats() {
        assertArrayEquals(
            arrayOf("image/jpeg", "image/png", "image/webp", "image/gif"),
            supportedAvatarMimeTypes(),
        )
    }

    private fun bytes(vararg values: Int) = ByteArray(values.size) { index ->
        values[index].toByte()
    }
}
