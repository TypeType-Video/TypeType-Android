package dev.typetype.android.domain.actions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockedKeywordTest {
    @Test
    fun matchingIgnoresCaseAndNormalizesUnicodeWidth() {
        assertTrue(titleMatchesBlockedKeyword("A SPOILER inside", listOf("spoiler")))
        assertTrue(titleMatchesBlockedKeyword("ＳＰＯＩＬＥＲ inside", listOf("spoiler")))
    }

    @Test
    fun blankAndUnrelatedKeywordsDoNotMatch() {
        assertFalse(titleMatchesBlockedKeyword("A normal title", listOf("", "   ", "spoiler")))
    }
}
