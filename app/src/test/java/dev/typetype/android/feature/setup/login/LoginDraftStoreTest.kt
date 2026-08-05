package dev.typetype.android.feature.setup.login

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LoginDraftStoreTest {
    @Test
    fun restoresIdentifierWithoutPersistingPassword() {
        val initialHandle = SavedStateHandle()
        LoginDraftStore(initialHandle).setIdentifier("priveetee@example.com")

        val restoredHandle = SavedStateHandle(
            initialHandle.keys().associateWith { key -> initialHandle.get<String>(key) },
        )

        assertEquals(
            "priveetee@example.com",
            LoginDraftStore(restoredHandle).restoreIdentifier(),
        )
        assertFalse(restoredHandle.keys().any { it.contains("password", ignoreCase = true) })
    }

    @Test
    fun clearingDraftRemovesTheSavedIdentifier() {
        val handle = SavedStateHandle()
        val store = LoginDraftStore(handle)
        store.setIdentifier("priveetee@example.com")

        store.clear()

        assertEquals("", store.restoreIdentifier())
        assertEquals(emptySet<String>(), handle.keys())
    }
}
