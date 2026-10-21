package dev.typetype.android.data.diagnostics

import dev.typetype.android.data.account.AccountScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushDiagnosticRecorder @Inject constructor(
    private val diagnostics: LocalDiagnosticsRepository,
) {
    fun record(
        scope: AccountScope,
        event: PushDiagnosticEvent,
        failureCode: String? = null,
        traceId: String? = null,
    ) {
        try {
            diagnostics.recordLocalEvent(
                serverId = scope.serverId,
                accountId = scope.accountId,
                route = event.route,
                failureCode = failureCode,
                traceId = traceId,
            )
        } catch (_: Exception) {
            return
        }
    }

    fun recordCurrent(
        event: PushDiagnosticEvent,
        failureCode: String? = null,
    ) {
        try {
            diagnostics.recordLocalEvent(
                route = event.route,
                failureCode = failureCode,
            )
        } catch (_: Exception) {
            return
        }
    }

    fun traceId(eventId: String): String? =
        eventId.takeIf(EVENT_ID_PATTERN::matches)?.take(16)

    private companion object {
        val EVENT_ID_PATTERN = Regex("[a-fA-F0-9]{64}")
    }
}

enum class PushDiagnosticEvent(val route: String) {
    InstanceInvalid("/push/instance/invalid"),
    RegistrationRequested("/push/registration/requested"),
    EndpointReceived("/push/registration/endpoint"),
    RegistrationActive("/push/registration/registered"),
    RegistrationFailed("/push/registration/failed"),
    RegistrationDisabled("/push/registration/disabled"),
    MessageReceived("/push/message/received"),
    MessageInvalid("/push/message/invalid"),
    NotificationPosted("/push/notification/posted"),
    NotificationSuppressed("/push/notification/suppressed"),
    NotificationFailed("/push/notification/failed"),
    DistributorUnavailable("/push/distributor/unavailable"),
    LocalPollChecked("/notifications/local/checked"),
    LocalNotificationPosted("/notifications/local/posted"),
    LocalNotificationSuppressed("/notifications/local/suppressed"),
    LocalNotificationFailed("/notifications/local/failed"),
    DeliveryAcknowledgementSent("/push/delivery/acknowledged"),
    DeliveryAcknowledgementFailed("/push/delivery/acknowledgement-failed"),
}
