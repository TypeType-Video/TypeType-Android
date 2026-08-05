package dev.typetype.android.feature.settings.accounts

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.domain.account.Account
import dev.typetype.android.domain.server.Server
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class InstanceAccountsCardTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun memberAccountOffersIdentityBoundReauthentication() {
        var requestedAccountId: String? = null
        composeRule.setContent {
            MaterialTheme {
                InstanceAccountsCard(
                    server = server,
                    accounts = listOf(member),
                    activeScope = AccountScope(server.id, member.id),
                    busyAccountId = null,
                    onSelect = { _, _ -> },
                    onForget = { _, _ -> },
                    onSignIn = { requestedAccountId = it },
                )
            }
        }

        composeRule.onNodeWithText("Sign in again").assertIsDisplayed().performClick()

        composeRule.runOnIdle { assertEquals(member.id, requestedAccountId) }
    }

    @Test
    fun guestAccountDoesNotOfferReauthentication() {
        composeRule.setContent {
            MaterialTheme {
                InstanceAccountsCard(
                    server = server,
                    accounts = listOf(member.copy(id = "guest", isGuest = true)),
                    activeScope = AccountScope(server.id, "guest"),
                    busyAccountId = null,
                    onSelect = { _, _ -> },
                    onForget = { _, _ -> },
                    onSignIn = {},
                )
            }
        }

        composeRule.onAllNodesWithText("Sign in again").assertCountEquals(0)
        composeRule.onNodeWithText("Sign in with another account").assertIsDisplayed()
    }

    private val server = Server(
        id = "server",
        baseUrl = "https://instance.example/api/",
        displayName = "Instance",
        addedAt = 1L,
    )

    private val member = Account(
        serverId = server.id,
        id = "member",
        publicUsername = "Member",
        role = "user",
        avatarUrl = null,
        avatarType = null,
        avatarCode = null,
        isGuest = false,
        lastUsedAt = 1L,
    )
}
