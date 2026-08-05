package dev.typetype.android.feature.setup.register

import androidx.lifecycle.SavedStateHandle

internal class RegistrationDraftStore(
    private val savedStateHandle: SavedStateHandle,
) {
    fun restore() = RegistrationDraft(
        name = savedStateHandle.get<String>(NAME_KEY).orEmpty(),
        email = savedStateHandle.get<String>(EMAIL_KEY).orEmpty(),
    )

    fun setName(value: String) {
        savedStateHandle[NAME_KEY] = value
    }

    fun setEmail(value: String) {
        savedStateHandle[EMAIL_KEY] = value
    }

    fun clear() {
        savedStateHandle.remove<String>(NAME_KEY)
        savedStateHandle.remove<String>(EMAIL_KEY)
    }
}

internal data class RegistrationDraft(
    val name: String,
    val email: String,
)

private const val NAME_KEY = "registration.name"
private const val EMAIL_KEY = "registration.email"
