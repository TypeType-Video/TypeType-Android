package dev.typetype.android.data.feed

import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.domain.server.RecommenderCapability
import dev.typetype.android.domain.server.Server
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationAvailabilityTest {
    @Test
    fun requiresAnAuthenticatedAccountAndAdvertisedCapability() {
        assertFalse(scope().canUseRecommendations(null))
        assertFalse(scope().canUseRecommendations(server(installed = false, enabled = true)))
        assertFalse(scope().canUseRecommendations(server(installed = true, enabled = false)))
        assertTrue(scope().canUseRecommendations(server(installed = true, enabled = true)))
    }

    @Test
    fun guestAccountsNeverRequestPersonalRecommendations() {
        assertFalse(scope(accountId = "guest:anonymous").canUseRecommendations(
            server(installed = true, enabled = true),
        ))
    }

    private fun scope(accountId: String = "account") = AccountScope("server", accountId)

    private fun server(installed: Boolean, enabled: Boolean) = Server(
        id = "server",
        baseUrl = "https://example.test/api/",
        displayName = "Test",
        addedAt = 1L,
        recommender = RecommenderCapability(installed = installed, enabled = enabled),
    )
}
