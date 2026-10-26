package dev.typetype.android.data.recommendations

import dev.typetype.android.core.error.suspendRunCatching
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApi
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.recommendations.AlgorithmCatalog
import dev.typetype.android.domain.recommendations.DataCategory
import dev.typetype.android.domain.recommendations.FeedbackEntry
import dev.typetype.android.domain.recommendations.PageTrace
import dev.typetype.android.domain.recommendations.RecommenderPreferences
import dev.typetype.android.domain.recommendations.RecommenderProfile
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

    override suspend fun profile(locale: String): Result<RecommenderProfile> =
        call { recommenderProfile(locale, TimeZone.getDefault().id) }.map { it.toProfile() }

    override suspend fun data(): Result<List<DataCategory>> =
        call { recommenderData() }.map { it.toDataCategories() }

    override suspend fun lastHome(): Result<PageTrace?> =
        callOrNull(NOT_FOUND) { recommenderLastHome() }.map { it?.toPageTrace() }

    override suspend fun feedback(): Result<List<FeedbackEntry>> =
        call { recommenderFeedback() }.map { it.toFeedbackEntries() }

    private suspend fun <T> call(request: suspend TypeTypeApi.() -> Response<T>): Result<T> =
        callOrNull(null, request).map { requireNotNull(it) { "Empty recommender response" } }

    private suspend fun <T> callOrNull(
        absentCode: Int?,
        request: suspend TypeTypeApi.() -> Response<T>,
    ): Result<T?> = suspendRunCatching {
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) { apiHolder.require(scope).request() }
        activeAccountScope.verify(scope)
        if (absentCode != null && response.code() == absentCode) return@suspendRunCatching null
        response.requireSuccessfulResponse()
        response.body()
    }

    private companion object {
        const val NOT_FOUND = 404
    }
}
