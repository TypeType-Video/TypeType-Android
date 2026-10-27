package dev.typetype.android.feature.subscriptions

import dev.typetype.android.domain.actions.BlockedItem
import dev.typetype.android.domain.subscriptions.SubscriptionSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionChannelsProviderTest {
    @Test
    fun channelsAreSortedAndBlockedIdentitiesAreHidden() {
        val subscriptions = listOf(
            subscription("z", "Zulu"),
            subscription("blocked-url", "Alpha"),
            subscription("b", "Blocked by name"),
            subscription("a", "Bravo"),
        )
        val blocked = listOf(
            blocked("blocked-url", ""),
            blocked("different-url", "blocked BY NAME"),
        )

        val visible = visibleSubscriptionChannels(subscriptions, blocked)

        assertEquals(listOf("Bravo", "Zulu"), visible.map { it.name })
    }

    private fun subscription(url: String, name: String) = SubscriptionSummary(
        channelUrl = url,
        name = name,
        avatarUrl = "",
        subscribedAtMillis = 0,
    )

    private fun blocked(url: String, name: String) = BlockedItem(
        url = url,
        name = name,
        thumbnailUrl = "",
        blockedAt = 0,
    )
}
