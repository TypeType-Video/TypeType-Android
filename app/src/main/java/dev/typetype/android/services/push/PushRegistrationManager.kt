package dev.typetype.android.services.push

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.R
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.push.PushRegistrationStore
import dev.typetype.android.data.push.pushInstanceName
import dev.typetype.android.data.push.scopeFromInstanceName
import dev.typetype.android.domain.push.PushRegistrationStatus
import dev.typetype.android.domain.push.PushRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.unifiedpush.android.connector.UnifiedPush

@Singleton
class PushRegistrationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PushRepository,
    private val registrationStore: PushRegistrationStore,
    private val activeAccountScope: ActiveAccountScope,
) {
    private val _status = MutableStateFlow<PushRegistrationStatus>(PushRegistrationStatus.Disabled)
    val status: StateFlow<PushRegistrationStatus> = _status.asStateFlow()

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun enable(): PushRegistrationStatus {
        val scope = activeAccountScope.require()
        if (!repository.currentCapability().enabled) {
            return update(PushRegistrationStatus.Unavailable)
        }
        return startRegistration(scope)
    }

    suspend fun reconcileRegistration(): PushRegistrationStatus {
        val scope = activeAccountScope.require()
        val registration = registrationStore.registrationOnce(scope)
            ?: return update(PushRegistrationStatus.Disabled)
        if (!repository.currentCapability().enabled) {
            return update(PushRegistrationStatus.Unavailable)
        }
        val endpoint = registration.endpoint ?: return startRegistration(scope)
        val devices = repository.devices().getOrElse {
            return update(PushRegistrationStatus.Failed)
        }
        if (devices.any { device -> device.deviceId == registration.deviceId }) {
            return update(PushRegistrationStatus.Registered)
        }
        return repository.registerDevice(registration.deviceId, endpoint).fold(
            onSuccess = { update(PushRegistrationStatus.Registered) },
            onFailure = { update(PushRegistrationStatus.Failed) },
        )
    }

    private suspend fun startRegistration(scope: AccountScope): PushRegistrationStatus {
        val instance = pushInstanceName(scope)
        if (UnifiedPush.getDistributors(context).isEmpty()) {
            return update(PushRegistrationStatus.MissingDistributor)
        }
        if (UnifiedPush.getAckDistributor(context) == null) {
            UnifiedPush.saveDistributor(context, UnifiedPush.getDistributors(context).first())
        }
        registrationStore.ensureDeviceId(scope)
        UnifiedPush.register(context, instance, context.getString(R.string.app_name))
        return update(PushRegistrationStatus.Registering)
    }

    suspend fun disable(): PushRegistrationStatus {
        val scope = activeAccountScope.require()
        registrationStore.registrationOnce(scope)?.let { registration ->
            repository.unregisterDevice(registration.deviceId)
        }
        UnifiedPush.unregister(context, pushInstanceName(scope))
        registrationStore.clear(scope)
        return update(PushRegistrationStatus.Disabled)
    }

    suspend fun refreshStatus(): PushRegistrationStatus {
        val scope = activeAccountScope.require()
        if (!repository.currentCapability().enabled) {
            return update(PushRegistrationStatus.Unavailable)
        }
        val registration = registrationStore.registrationOnce(scope)
        return when {
            registration == null -> update(PushRegistrationStatus.Disabled)
            registration.endpoint != null -> update(PushRegistrationStatus.Registered)
            else -> update(PushRegistrationStatus.Registering)
        }
    }

    fun onEndpointAvailable(instance: String, endpoint: String) {
        managerScope.launch {
            val accountScope = scopeFromInstanceName(instance) ?: return@launch
            val deviceId = registrationStore.ensureDeviceId(accountScope)
            repository.registerDevice(deviceId, endpoint).fold(
                onSuccess = {
                    registrationStore.setEndpoint(accountScope, endpoint)
                    update(PushRegistrationStatus.Registered)
                },
                onFailure = { update(PushRegistrationStatus.Failed) },
            )
        }
    }

    fun onUnregistered(instance: String) {
        managerScope.launch {
            val accountScope = scopeFromInstanceName(instance) ?: return@launch
            registrationStore.registrationOnce(accountScope)?.let { registration ->
                repository.unregisterDevice(registration.deviceId)
            }
            registrationStore.clear(accountScope)
            update(PushRegistrationStatus.Disabled)
        }
    }

    fun onRegistrationFailed(instance: String) {
        if (scopeFromInstanceName(instance) != null) {
            update(PushRegistrationStatus.Failed)
        }
    }

    private fun update(status: PushRegistrationStatus): PushRegistrationStatus {
        _status.value = status
        return status
    }
}
