package dev.typetype.android.data.push

import dev.typetype.android.data.account.AccountDao
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.PushDeliveryAcknowledgementRequestDto
import dev.typetype.android.data.network.requireSuccessfulResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class PushDeliveryAcknowledgementRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val accountDao: AccountDao,
    private val registrationStore: PushRegistrationStore,
) {
    suspend fun acknowledge(
        scope: AccountScope,
        eventId: String,
        outcome: String,
        reasonCode: String?,
    ): Result<Unit> = try {
        val account = accountDao.get(scope.serverId, scope.accountId)
        check(account != null && !account.isGuest) { "Push acknowledgements require an account" }
        val deviceId = registrationStore.registrationOnce(scope)?.deviceId
            ?: error("The push device registration is missing")
        val api = apiHolder.require(scope)
        val response = withContext(Dispatchers.IO) {
            api.acknowledgePushDelivery(
                PushDeliveryAcknowledgementRequestDto(
                    eventId = eventId,
                    deviceId = deviceId,
                    outcome = outcome,
                    reasonCode = reasonCode,
                ),
            )
        }
        response.requireSuccessfulResponse()
        Result.success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }
}
