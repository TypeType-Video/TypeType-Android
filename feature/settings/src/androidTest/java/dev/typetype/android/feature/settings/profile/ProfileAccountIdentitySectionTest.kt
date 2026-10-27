package dev.typetype.android.feature.settings.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.profile.AccountIdentity
import org.junit.Rule
import org.junit.Test

class ProfileAccountIdentitySectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun localAccountRequiresTheCurrentPasswordBeforeSaving() {
        setSection(
            ProfileSettingsState(
                identity = identity(managedByOidc = false),
                emailDraft = "user@example.com",
                nameDraft = "User",
            ),
        )

        composeRule.onNodeWithText("Update account identity").assertIsNotEnabled()
    }

    @Test
    fun completeLocalIdentityCanBeSaved() {
        setSection(
            ProfileSettingsState(
                identity = identity(managedByOidc = false),
                emailDraft = "user@example.com",
                nameDraft = "User",
                currentPasswordDraft = "secret",
            ),
        )

        composeRule.onNodeWithText("Update account identity").assertIsEnabled()
    }

    @Test
    fun oidcIdentityExplainsWhyItCannotBeEdited() {
        setSection(
            ProfileSettingsState(
                identity = identity(managedByOidc = true),
                emailDraft = "user@example.com",
                nameDraft = "User",
            ),
        )

        composeRule.onNodeWithText(
            "Your identity is managed by your OIDC provider.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Update account identity").assertDoesNotExist()
    }

    private fun setSection(state: ProfileSettingsState) {
        composeRule.setContent {
            TypeTypeTheme {
                ProfileAccountIdentitySection(
                    state = state,
                    onEmailChange = {},
                    onNameChange = {},
                    onPasswordChange = {},
                    onSave = {},
                    onResetPassword = {},
                    onRetry = {},
                )
            }
        }
    }

    private fun identity(managedByOidc: Boolean) = AccountIdentity(
        email = "user@example.com",
        name = "User",
        managedByOidc = managedByOidc,
    )
}
