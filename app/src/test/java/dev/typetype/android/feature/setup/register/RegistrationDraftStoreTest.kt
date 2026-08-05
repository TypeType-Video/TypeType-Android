package dev.typetype.android.feature.setup.register

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RegistrationDraftStoreTest {
    @Test
    fun restoresNonSecretFieldsWithoutPersistingPassword() {
        val initialHandle = SavedStateHandle()
        RegistrationDraftStore(initialHandle).apply {
            setName("Priveetee")
            setEmail("hello@example.com")
        }

        val restoredHandle = SavedStateHandle(
            initialHandle.keys().associateWith { key -> initialHandle.get<String>(key) },
        )
        val restored = RegistrationDraftStore(restoredHandle).restore()

        assertEquals("Priveetee", restored.name)
        assertEquals("hello@example.com", restored.email)
        assertFalse(restoredHandle.keys().any { it.contains("password", ignoreCase = true) })
    }

    @Test
    fun clearingDraftRemovesEverySavedField() {
        val handle = SavedStateHandle()
        val store = RegistrationDraftStore(handle)
        store.setName("Priveetee")
        store.setEmail("hello@example.com")

        store.clear()

        assertEquals(RegistrationDraft(name = "", email = ""), store.restore())
        assertEquals(emptySet<String>(), handle.keys())
    }
}
