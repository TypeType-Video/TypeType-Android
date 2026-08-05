package dev.typetype.android.feature.setup.login

import androidx.lifecycle.SavedStateHandle

internal class LoginDraftStore(
    private val savedStateHandle: SavedStateHandle,
) {
    fun restoreIdentifier(): String = savedStateHandle.get<String>(IDENTIFIER_KEY).orEmpty()

    fun setIdentifier(value: String) {
        savedStateHandle[IDENTIFIER_KEY] = value
    }

    fun clear() {
        savedStateHandle.remove<String>(IDENTIFIER_KEY)
    }
}

private const val IDENTIFIER_KEY = "login.identifier"
