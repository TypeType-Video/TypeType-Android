package dev.typetype.android.feature.setup.auth

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OidcAuthLauncherTest {
    @Test
    fun `uses auth tab when available`() {
        val launches = mutableListOf<String>()

        val launched = launchOidcWithBrowserFallback(
            launchAuthTab = {
                launches += "auth-tab"
                true
            },
            launchBrowser = {
                launches += "browser"
                true
            },
        )

        assertTrue(launched)
        assertEquals(listOf("auth-tab"), launches)
    }

    @Test
    fun `falls back to the external browser`() {
        val launches = mutableListOf<String>()

        val launched = launchOidcWithBrowserFallback(
            launchAuthTab = {
                launches += "auth-tab"
                false
            },
            launchBrowser = {
                launches += "browser"
                true
            },
        )

        assertTrue(launched)
        assertEquals(listOf("auth-tab", "browser"), launches)
    }

    @Test
    fun `reports when no external browser can open the flow`() {
        val launched = launchOidcWithBrowserFallback(
            launchAuthTab = { false },
            launchBrowser = { false },
        )

        assertFalse(launched)
    }

    @Test
    fun `keeps the transaction when a callback arrives after browser return`() = runBlocking {
        var callbackReceived = false
        var cancelled = false
        launch {
            delay(5)
            callbackReceived = true
        }

        cancelOidcAfterBrowserReturn(
            hasCallback = { callbackReceived },
            cancel = { cancelled = true },
            delayMillis = 20,
        )

        assertFalse(cancelled)
    }

    @Test
    fun `cleans the transaction after a real browser cancellation`() = runBlocking {
        var cancelled = false

        cancelOidcAfterBrowserReturn(
            hasCallback = { false },
            cancel = { cancelled = true },
            delayMillis = 0,
        )

        assertTrue(cancelled)
    }
}
