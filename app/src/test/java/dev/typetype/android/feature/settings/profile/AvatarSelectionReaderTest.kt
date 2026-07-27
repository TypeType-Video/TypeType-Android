package dev.typetype.android.feature.settings.profile

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
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
}
