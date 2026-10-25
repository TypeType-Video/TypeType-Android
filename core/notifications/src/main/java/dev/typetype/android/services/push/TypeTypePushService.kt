package dev.typetype.android.services.push

import dagger.hilt.android.AndroidEntryPoint
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.diagnostics.PushDiagnosticEvent
import dev.typetype.android.data.diagnostics.PushDiagnosticRecorder
import dev.typetype.android.data.push.scopeFromInstanceName
import dev.typetype.android.domain.push.PushPayload
import javax.inject.Inject
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage

@AndroidEntryPoint
class TypeTypePushService : PushService() {
    @Inject lateinit var registrationManager: PushRegistrationManager

    @Inject lateinit var notifier: PushNotifier

    @Inject lateinit var pushDiagnostics: PushDiagnosticRecorder

    override fun onNewEndpoint(endpoint: PushEndpoint, instance: String) {
        val scope = scopeFromInstanceName(instance)
        if (scope == null) {
            pushDiagnostics.recordCurrent(PushDiagnosticEvent.InstanceInvalid, "unknown_instance")
            registrationManager.onRegistrationFailed(instance)
            return
        }
        val keySet = endpoint.pubKeySet
        if (keySet == null || endpoint.url.isBlank() || keySet.pubKey.isBlank() || keySet.auth.isBlank()) {
            pushDiagnostics.record(
                scope,
                PushDiagnosticEvent.RegistrationFailed,
                failureCode = "endpoint_missing_keys",
            )
            registrationManager.onRegistrationFailed(instance)
            return
        }
        pushDiagnostics.record(scope, PushDiagnosticEvent.EndpointReceived)
        registrationManager.onEndpointAvailable(instance, endpoint.url, keySet.pubKey, keySet.auth)
    }

    override fun onMessage(message: PushMessage, instance: String) {
        when (val classification = classifyPushMessage(instance, message.content)) {
            is PushMessageClassification.Rejected -> {
                val scope = classification.scope
                if (scope == null) {
                    pushDiagnostics.recordCurrent(PushDiagnosticEvent.InstanceInvalid, classification.failureCode)
                } else {
                    pushDiagnostics.record(scope, PushDiagnosticEvent.MessageReceived)
                    pushDiagnostics.record(
                        scope,
                        PushDiagnosticEvent.MessageInvalid,
                        failureCode = classification.failureCode,
                    )
                }
            }
            is PushMessageClassification.Accepted -> showNotification(classification.scope, classification.payload)
        }
    }

    private fun showNotification(
        scope: AccountScope,
        payload: PushPayload,
    ) {
        pushDiagnostics.record(scope, PushDiagnosticEvent.MessageReceived)
        val traceId = pushDiagnostics.traceId(payload.eventId)
        val result = try {
            notifier.notify(scope, payload)
        } catch (_: Exception) {
            NotificationPostResult.Failed
        }
        when (result) {
            NotificationPostResult.Posted -> pushDiagnostics.record(
                scope,
                PushDiagnosticEvent.NotificationPosted,
                traceId = traceId,
            )
            NotificationPostResult.NotificationsDisabled -> pushDiagnostics.record(
                scope,
                PushDiagnosticEvent.NotificationSuppressed,
                failureCode = "notifications_disabled",
                traceId = traceId,
            )
            NotificationPostResult.ChannelBlocked -> pushDiagnostics.record(
                scope,
                PushDiagnosticEvent.NotificationSuppressed,
                failureCode = "channel_blocked",
                traceId = traceId,
            )
            NotificationPostResult.PermissionDenied -> pushDiagnostics.record(
                scope,
                PushDiagnosticEvent.NotificationSuppressed,
                failureCode = "permission_denied",
                traceId = traceId,
            )
            NotificationPostResult.Failed -> pushDiagnostics.record(
                scope,
                PushDiagnosticEvent.NotificationFailed,
                failureCode = "notification_manager_unavailable",
                traceId = traceId,
            )
        }
        registrationManager.acknowledgeDelivery(scope, payload.eventId, result)
    }

    override fun onRegistrationFailed(reason: FailedReason, instance: String) {
        registrationManager.onRegistrationFailed(instance, reason.diagnosticFailureCode())
    }

    override fun onTempUnavailable(instance: String) {
        val scope = scopeFromInstanceName(instance)
        if (scope == null) {
            pushDiagnostics.recordCurrent(PushDiagnosticEvent.InstanceInvalid, "unknown_instance")
            return
        }
        pushDiagnostics.record(
            scope,
            PushDiagnosticEvent.DistributorUnavailable,
            failureCode = "distributor_temporarily_unavailable",
        )
    }

    override fun onUnregistered(instance: String) {
        registrationManager.onUnregistered(instance)
    }
}

internal fun FailedReason.diagnosticFailureCode(): String = when (name) {
    "NETWORK" -> "unifiedpush_network"
    "INTERNAL_ERROR" -> "unifiedpush_internal_error"
    "ACTION_REQUIRED" -> "unifiedpush_action_required"
    "VAPID_REQUIRED" -> "unifiedpush_vapid_required"
    else -> "unifiedpush_registration_failed"
}
