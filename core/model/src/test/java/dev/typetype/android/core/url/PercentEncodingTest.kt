package dev.typetype.android.core.url

import org.junit.Assert.assertEquals
import org.junit.Test

class PercentEncodingTest {
    @Test
    fun `keeps unreserved characters`() {
        assertEquals("aZ0-_.~", percentEncode("aZ0-_.~"))
    }

    @Test
    fun `escapes reserved query characters`() {
        assertEquals(
            "https%3A%2F%2Fexample.test%2Fwatch%3Fv%3Da%26b%3Dc",
            percentEncode("https://example.test/watch?v=a&b=c"),
        )
    }

    @Test
    fun `escapes spaces and multibyte characters as utf8`() {
        assertEquals("a%20b", percentEncode("a b"))
        assertEquals("%C3%A9t%C3%A9", percentEncode("été"))
    }
}
