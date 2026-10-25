package dev.typetype.android.data.recommendations

import dev.typetype.android.core.error.suspendRunCatching
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApi
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.recommendations.AlgorithmCatalog
import dev.typetype.android.domain.recommendations.DataFamily
import dev.typetype.android.domain.recommendations.InterestGroup
import dev.typetype.android.domain.recommendations.RecommenderPreferences
import dev.typetype.android.domain.recommendations.RecommenderRepository
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

@Singleton
class RemoteRecommenderRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
) : RecommenderRepository {
    override suspend fun catalog(locale: String): Result<AlgorithmCatalog> =
        call { recommenderAlgorithms(locale, TimeZone.getDefault().id) }.map { it.toCatalog() }

    override suspend fun preferences(): Result<RecommenderPreferences> =
        call { recommenderSettings() }.map { it.toPreferences() }

    override suspend fun save(preferences: RecommenderPreferences): Result<RecommenderPreferences> =
        suspendRunCatching {
            val current = call { recommenderSettings() }.getOrThrow()
            call { updateRecommenderSettings(current.withPreferences(preferences)) }.getOrThrow().toPreferences()
        }

    override suspend fun interests(locale: String): Result<List<InterestGroup>> =
        call { recommenderProfile(locale, TimeZone.getDefault().id) }.map { it.toInterests() }

    override suspend fun dataFamilies(): Result<List<DataFamily>> =
        call { recommenderData() }.map { it.toDataFamilies() }

    private suspend fun <T> call(request: suspend TypeTypeApi.() -> Response<T>): Result<T> = suspendRunCatching {
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) { apiHolder.require(scope).request() }
        response.requireSuccessfulResponse()
        val body = requireNotNull(response.body()) { "Empty recommender response" }
        activeAccountScope.verify(scope)
        body
    }
}
