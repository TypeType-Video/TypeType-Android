package dev.typetype.android.feature.library

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryStateTest {
    @Test
    fun `first empty load uses the full screen loader`() {
        assertTrue(LibraryState(isLoading = true).shouldShowInitialLoader())
    }

    @Test
    fun `cached content remains visible during refresh`() {
        val state = LibraryState(
            isLoading = true,
            historyItemCount = 1,
        )

        assertFalse(state.shouldShowInitialLoader())
    }

    @Test
    fun `known empty collection uses inline refresh state`() {
        val state = LibraryState(
            isLoading = true,
            lastSuccessfulSyncAtMillis = 10L,
        )

        assertFalse(state.shouldShowInitialLoader())
    }
}
