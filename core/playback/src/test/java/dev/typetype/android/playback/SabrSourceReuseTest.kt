package dev.typetype.android.playback

import dev.typetype.android.playback.PlayableSource
import dev.typetype.android.playback.SabrFixtures.binding
import dev.typetype.android.playback.SabrFixtures.requestKey
import dev.typetype.android.playback.SabrFixtures.sabrStream
import dev.typetype.android.playback.SabrFixtures.target
import dev.typetype.android.playback.reusableSabrSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SabrSourceReuseTest {
    @Test
    fun `prepared SABR source is reused for the same requested tuple`() {
        val source = reusableSabrSource(
            requestKey = "sabr:video:137:140:en.0",
            storedRequestKey = "sabr:video:137:140:en.0",
            acceptedKey = "sabr:video:136:140:en.0:video",
            url = "typetype-sabr://playback/session",
            sabrBinding = binding(videoItag = 136),
            sabrTarget = target(videoItag = 136),
        )

        requireNotNull(source)
        assertEquals("sabr:video:136:140:en.0:video", source.sourceKey)
        assertEquals("sabr:video:137:140:en.0", source.sabrRequestKey)
    }

    @Test
    fun `prepared SABR source is not reused for another requested tuple`() {
        val source = reusableSabrSource(
            requestKey = "sabr:video:135:140:en.0",
            storedRequestKey = "sabr:video:137:140:en.0",
            acceptedKey = "sabr:video:136:140:en.0:video",
            url = "typetype-sabr://playback/session",
            sabrBinding = binding(videoItag = 136),
            sabrTarget = target(videoItag = 136),
        )

        assertNull(source)
    }

    @Test
    fun `prepared SABR source without its accepted binding is not reused`() {
        val source = reusableSabrSource(
            requestKey = "sabr:video:137:140:en.0",
            storedRequestKey = "sabr:video:137:140:en.0",
            acceptedKey = "sabr:video:137:140:en.0:video",
            url = "typetype-sabr://playback/session",
            sabrBinding = null,
            sabrTarget = target(),
        )

        assertNull(source)
    }

    @Test
    fun `prepared SABR source is not reused across accounts`() {
        val firstAccount = sabrStream()
        val secondAccount = firstAccount.copy(
            requestScope = firstAccount.requestScope?.copy(accountId = "another-account"),
        )
        val firstKey = requestKey(firstAccount)
        val secondKey = requestKey(secondAccount)

        assertFalse(firstKey == secondKey)
        assertNull(
            reusableSabrSource(
                requestKey = secondKey,
                storedRequestKey = firstKey,
                acceptedKey = "sabr:video:137:140:en.0:video",
                url = "typetype-sabr://playback/session",
                sabrBinding = binding(),
                sabrTarget = target(),
            ),
        )
    }

    @Test
    fun `fresh SABR session replaces the failed source for the same tuple`() {
        val replacement = PlayableSource(
            url = "typetype-sabr://playback/fresh",
            mimeType = "application/x-typetype-sabr",
            sourceKey = "sabr:video:137:140:en.0:video",
            sabrRequestKey = "sabr:video:137:140:en.0",
        )

        assertFalse(
            samePlayableSource(
                currentUrl = "typetype-sabr://playback/failed",
                currentSourceKey = replacement.sourceKey,
                currentAudioUrl = null,
                requestedSource = replacement,
            ),
        )
    }
}
