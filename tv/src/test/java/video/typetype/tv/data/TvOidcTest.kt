package video.typetype.tv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TvOidcTest {
    @Test
    fun authorizationRequiresHttpsAndKnownCallback() {
        val authorization = parseOidcAuthorization(
            "https://login.example/authorize?state=state-1&redirect_uri=video.typetype.tv%3A%2F%2Fauth%2Fcallback",
        )

        assertEquals("state-1", authorization?.state)
        assertEquals(TV_OIDC_REDIRECT_URI, authorization?.redirectUri)
        assertNull(parseOidcAuthorization("http://login.example/authorize?state=state-1"))
        assertNull(parseOidcAuthorization("https://login.example/authorize?state=state-1&redirect_uri=https%3A%2F%2Fexample"))
    }

    @Test
    fun callbackRequiresExpectedStateAndExactRoute() {
        val callback = parseOidcCallback(
            "video.typetype.tv://auth/callback?code=code-1&state=state-1",
            expectedState = "state-1",
            redirectUri = TV_OIDC_REDIRECT_URI,
        )

        assertEquals("code-1", callback?.code)
        assertNull(
            parseOidcCallback(
                "video.typetype.tv://auth/callback?code=code-1&state=other",
                expectedState = "state-1",
                redirectUri = TV_OIDC_REDIRECT_URI,
            ),
        )
        assertNull(
            parseOidcCallback(
                "video.typetype.tv://other/callback?code=code-1&state=state-1",
                expectedState = "state-1",
                redirectUri = TV_OIDC_REDIRECT_URI,
            ),
        )
    }
}
