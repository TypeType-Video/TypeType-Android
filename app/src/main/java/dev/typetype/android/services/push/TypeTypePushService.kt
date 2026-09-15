package dev.typetype.android.services.push

import dagger.hilt.android.AndroidEntryPoint
import dev.typetype.android.domain.push.parsePushPayload
import javax.inject.Inject
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage

@AndroidEntryPoint
class TypeTypePushService : PushService() {
    @Inject lateinit var registrationManager: PushRegistrationManager

    @Inject lateinit var notifier: PushNotifier

    override fun onNewEndpoint(endpoint: PushEndpoint, instance: String) {
        registrationManager.onEndpointAvailable(instance, endpoint.url)
    }

    override fun onMessage(message: PushMessage, instance: String) {
        val payload = parsePushPayload(message.content) ?: return
        notifier.notify(payload)
    }

    override fun onRegistrationFailed(reason: FailedReason, instance: String) {
        registrationManager.onRegistrationFailed(instance)
    }

    override fun onUnregistered(instance: String) {
        registrationManager.onUnregistered(instance)
    }
}
