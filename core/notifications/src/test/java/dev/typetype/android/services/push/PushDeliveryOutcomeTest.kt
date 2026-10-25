package dev.typetype.android.services.push

import org.junit.Assert.assertEquals
import org.junit.Test

class PushDeliveryOutcomeTest {
    @Test
    fun `posted notification is acknowledged as posted`() {
        assertEquals(PushDeliveryOutcome("posted", null), NotificationPostResult.Posted.toPushDeliveryOutcome())
    }

    @Test
    fun `disabled notification settings are acknowledged as suppressed`() {
        assertEquals(
            PushDeliveryOutcome("suppressed", "notifications_disabled"),
            NotificationPostResult.NotificationsDisabled.toPushDeliveryOutcome(),
        )
    }

    @Test
    fun `notification manager failure remains retryable`() {
        assertEquals(
            PushDeliveryOutcome("failed", "notification_manager_unavailable"),
            NotificationPostResult.Failed.toPushDeliveryOutcome(),
        )
    }
}
