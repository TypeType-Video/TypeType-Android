package dev.typetype.android.services.push

import dagger.hilt.android.AndroidEntryPoint
import dev.typetype.android.domain.push.parsePushPayload
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage

@AndroidEntryPoint
class TypeTypePushService : PushService() {
    @Inject lateinit var registrationManager: PushRegistrationManager

    @Inject lateinit var notifier: PushNotifier

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewEndpoint(endpoint: PushEndpoint, instance: String) {
        serviceScope.launch { registrationManager.onEndpointAvailable(instance, endpoint.url) }
    }

    override fun onMessage(message: PushMessage, instance: String) {
        val payload = parsePushPayload(message.content) ?: return
        serviceScope.launch { notifier.notify(payload) }
    }

    override fun onRegistrationFailed(reason: FailedReason, instance: String) {
        serviceScope.launch { registrationManager.onRegistrationFailed(instance) }
    }

    override fun onUnregistered(instance: String) {
        serviceScope.launch { registrationManager.onUnregistered(instance) }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
