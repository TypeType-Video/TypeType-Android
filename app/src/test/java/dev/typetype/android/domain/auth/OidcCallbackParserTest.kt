package dev.typetype.android.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OidcCallbackParserTest {
    @Test
    fun `parses the exact callback destination`() {
        val callback = OidcCallbackParser.parse(
            callbackUrl = "dev.typetype.android://oidc/callback?code=a%2Fb&state=s%2B1",
            expectedScheme = "dev.typetype.android",
        )

        assertEquals("a/b", callback.code)
        assertEquals("s+1", callback.state)
    }

    @Test
    fun `rejects another scheme host or path`() {
        assertThrows(IllegalArgumentException::class.java) {
            OidcCallbackParser.parse(
                "attacker://oidc/callback?code=a&state=b",
                "dev.typetype.android",
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            OidcCallbackParser.parse(
                "dev.typetype.android://other/callback?code=a&state=b",
                "dev.typetype.android",
            )
        }
    }

    @Test
    fun `surfaces provider errors and incomplete callbacks`() {
        assertThrows(IllegalStateException::class.java) {
            OidcCallbackParser.parse(
                "dev.typetype.android://oidc/callback?error=access_denied",
                "dev.typetype.android",
            )
        }
        assertThrows(IllegalStateException::class.java) {
            OidcCallbackParser.parse(
                "dev.typetype.android://oidc/callback?code=a",
                "dev.typetype.android",
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            OidcCallbackParser.parse(
                "dev.typetype.android://oidc/callback?code=a&code=b&state=c",
                "dev.typetype.android",
            )
        }
    }

    @Test
    fun `recognizes only the application callback destination`() {
        assertTrue(OidcRedirect.matches("${OidcRedirect.uri}?code=a&state=b"))
        assertFalse(OidcRedirect.matches("${OidcRedirect.scheme}://other/callback?code=a&state=b"))
        assertFalse(OidcRedirect.matches("https://example.com/oidc/callback?code=a&state=b"))
    }
}
