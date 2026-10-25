package dev.typetype.android.services.push

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.R
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.diagnostics.PushDiagnosticEvent
import dev.typetype.android.data.diagnostics.PushDiagnosticRecorder
import dev.typetype.android.data.push.PushRegistrationStore
import dev.typetype.android.data.push.PushDeliveryAcknowledgementRepository
import dev.typetype.android.data.push.pushInstanceName
import dev.typetype.android.data.push.scopeFromInstanceName
import dev.typetype.android.domain.push.PushRegistrationStatus
import dev.typetype.android.domain.push.PushRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.unifiedpush.android.connector.UnifiedPush

@Singleton
class PushRegistrationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PushRepository,
    private val registrationStore: PushRegistrationStore,
    private val deliveryAcknowledgements: PushDeliveryAcknowledgementRepository,
    private val activeAccountScope: ActiveAccountScope,
    private val pushDiagnostics: PushDiagnosticRecorder,
) {
    private val _status = MutableStateFlow<PushRegistrationStatus>(PushRegistrationStatus.Disabled)
    val status: StateFlow<PushRegistrationStatus> = _status.asStateFlow()

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun enable(): PushRegistrationStatus {
        val scope = activeAccountScope.require()
        if (!repository.currentCapability().enabled) {
            pushDiagnostics.record(scope, PushDiagnosticEvent.RegistrationFailed, "server_push_unavailable")
            return update(PushRegistrationStatus.Unavailable)
        }
        return startRegistration(scope)
    }

    suspend fun reconcileRegistration(): PushRegistrationStatus {
        val scope = activeAccountScope.require()
        val registration = registrationStore.registrationOnce(scope)
            ?: return update(PushRegistrationStatus.Disabled)
        if (!repository.currentCapability().enabled) {
            pushDiagnostics.record(scope, PushDiagnosticEvent.RegistrationFailed, "server_push_unavailable")
            return update(PushRegistrationStatus.Unavailable)
        }
        val endpoint = registration.endpoint ?: return startRegistration(scope)
        val p256dh = registration.p256dh
        val auth = registration.auth
        if (p256dh.isNullOrBlank() || auth.isNullOrBlank()) return startRegistration(scope)
        return repository.registerDevice(registration.deviceId, endpoint, p256dh, auth).fold(
            onSuccess = {
                pushDiagnostics.record(scope, PushDiagnosticEvent.RegistrationActive)
                update(PushRegistrationStatus.Registered)
            },
            onFailure = {
                pushDiagnostics.record(scope, PushDiagnosticEvent.RegistrationFailed, "server_registration_failed")
                update(PushRegistrationStatus.Failed)
            },
        )
    }

    private suspend fun startRegistration(scope: AccountScope): PushRegistrationStatus {
        val instance = pushInstanceName(scope)
        if (UnifiedPush.getDistributors(context).isEmpty()) {
            pushDiagnostics.record(scope, PushDiagnosticEvent.RegistrationFailed, "missing_distributor")
            return update(PushRegistrationStatus.MissingDistributor)
        }
        return try {
            if (UnifiedPush.getAckDistributor(context) == null) {
                UnifiedPush.saveDistributor(context, UnifiedPush.getDistributors(context).first())
            }
            registrationStore.ensureDeviceId(scope)
            pushDiagnostics.record(scope, PushDiagnosticEvent.RegistrationRequested)
            UnifiedPush.register(context, instance, context.getString(R.string.app_name))
            update(PushRegistrationStatus.Registering)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            pushDiagnostics.record(scope, PushDiagnosticEvent.RegistrationFailed, "unifiedpush_request_failed")
            update(PushRegistrationStatus.Failed)
        }
    }

    suspend fun disable(): PushRegistrationStatus {
        val scope = activeAccountScope.require()
        registrationStore.registrationOnce(scope)?.let { registration ->
            repository.unregisterDevice(registration.deviceId)
        }
        UnifiedPush.unregister(context, pushInstanceName(scope))
        registrationStore.clear(scope)
        pushDiagnostics.record(scope, PushDiagnosticEvent.RegistrationDisabled)
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
            !registration.endpoint.isNullOrBlank() &&
                !registration.p256dh.isNullOrBlank() &&
                !registration.auth.isNullOrBlank() -> update(PushRegistrationStatus.Registered)
            else -> update(PushRegistrationStatus.Registering)
        }
    }

    fun onEndpointAvailable(instance: String, endpoint: String, p256dh: String, auth: String) {
        managerScope.launch {
            val accountScope = scopeFromInstanceName(instance) ?: return@launch
            val deviceId = try {
                registrationStore.ensureDeviceId(accountScope).also {
                    registrationStore.setSubscription(accountScope, endpoint, p256dh, auth)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                pushDiagnostics.record(
                    accountScope,
                    PushDiagnosticEvent.RegistrationFailed,
                    "registration_storage_failed",
                )
                update(PushRegistrationStatus.Failed)
                return@launch
            }
            repository.registerDevice(deviceId, endpoint, p256dh, auth).fold(
                onSuccess = {
                    pushDiagnostics.record(accountScope, PushDiagnosticEvent.RegistrationActive)
                    update(PushRegistrationStatus.Registered)
                },
                onFailure = {
                    pushDiagnostics.record(accountScope, PushDiagnosticEvent.RegistrationFailed, "server_registration_failed")
                    update(PushRegistrationStatus.Failed)
                },
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
            pushDiagnostics.record(accountScope, PushDiagnosticEvent.RegistrationDisabled)
            update(PushRegistrationStatus.Disabled)
        }
    }

    fun acknowledgeDelivery(scope: AccountScope, eventId: String, result: NotificationPostResult) {
        val outcome = result.toPushDeliveryOutcome()
        managerScope.launch {
            deliveryAcknowledgements.acknowledge(scope, eventId, outcome.value, outcome.reasonCode).fold(
                onSuccess = {
                    pushDiagnostics.record(
                        scope,
                        PushDiagnosticEvent.DeliveryAcknowledgementSent,
                        traceId = pushDiagnostics.traceId(eventId),
                    )
                },
                onFailure = {
                    pushDiagnostics.record(
                        scope,
                        PushDiagnosticEvent.DeliveryAcknowledgementFailed,
                        failureCode = "server_acknowledgement_failed",
                        traceId = pushDiagnostics.traceId(eventId),
                    )
                },
            )
        }
    }

    fun onRegistrationFailed(
        instance: String,
        failureCode: String = "unifiedpush_registration_failed",
    ) {
        val scope = scopeFromInstanceName(instance)
        if (scope != null) {
            pushDiagnostics.record(scope, PushDiagnosticEvent.RegistrationFailed, failureCode)
            update(
                if (failureCode == "unifiedpush_action_required") {
                    PushRegistrationStatus.ActionRequired
                } else {
                    PushRegistrationStatus.Failed
                },
            )
        }
    }

    private fun update(status: PushRegistrationStatus): PushRegistrationStatus {
        _status.value = status
        return status
    }
}
