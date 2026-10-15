package dev.typetype.android.data.recommendations

import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.recommendations.RecommenderSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RemoteRecommenderSettingsRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
) : RecommenderSettingsRepository {
    override suspend fun enabled(): Result<Boolean> = captureResult {
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) {
            apiHolder.require(scope).recommenderSettings()
        }
        response.requireSuccessfulResponse()
        val settings = requireNotNull(response.body()) { "Empty recommender settings" }
        activeAccountScope.verify(scope)
        settings.recommenderEnabled()
    }

    override suspend fun setEnabled(enabled: Boolean): Result<Boolean> = captureResult {
        val scope = activeAccountScope.require()
        val api = apiHolder.require(scope)
        val currentResponse = withContext(Dispatchers.IO) { api.recommenderSettings() }
        currentResponse.requireSuccessfulResponse()
        val current = requireNotNull(currentResponse.body()) { "Empty recommender settings" }
        activeAccountScope.verify(scope)
        val update = current.withRecommenderEnabled(enabled)
        val response = withContext(Dispatchers.IO) {
            api.updateRecommenderSettings(update)
        }
        response.requireSuccessfulResponse()
        val saved = requireNotNull(response.body()) { "Empty saved recommender settings" }
        activeAccountScope.verify(scope)
        saved.recommenderEnabled()
    }
}

private suspend fun <T> captureResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Throwable) {
    Result.failure(failure)
}
