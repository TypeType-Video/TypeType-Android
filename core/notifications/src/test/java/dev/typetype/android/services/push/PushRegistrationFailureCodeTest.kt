package dev.typetype.android.services.push

import org.junit.Assert.assertEquals
import org.junit.Test
import org.unifiedpush.android.connector.FailedReason

class PushRegistrationFailureCodeTest {
    @Test
    fun `maps distributor registration failures to stable diagnostic codes`() {
        assertEquals("unifiedpush_network", FailedReason.NETWORK.diagnosticFailureCode())
        assertEquals("unifiedpush_internal_error", FailedReason.INTERNAL_ERROR.diagnosticFailureCode())
        assertEquals("unifiedpush_action_required", FailedReason.ACTION_REQUIRED.diagnosticFailureCode())
        assertEquals("unifiedpush_vapid_required", FailedReason.VAPID_REQUIRED.diagnosticFailureCode())
    }
}
