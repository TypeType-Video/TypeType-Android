package dev.typetype.android.data.searchhistory

import dev.typetype.android.core.error.suspendRunCatching
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.SearchHistoryEntryRequest
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.searchhistory.SearchHistoryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RemoteSearchHistoryStore @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
) : SearchHistoryRepository {

    override suspend fun loadHistory(): Result<List<String>> = suspendRunCatching {
        val scope = activeAccountScope.require()
        val api = apiHolder.require(scope)
        val response = withContext(Dispatchers.IO) { api.searchHistory() }
        response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
        response.body().orEmpty()
            .map { it.term }
            .filter { it.isNotBlank() }
    }

    override suspend fun addEntry(query: String): Result<Unit> = suspendRunCatching {
        val scope = activeAccountScope.require()
        val api = apiHolder.require(scope)
        val response = withContext(Dispatchers.IO) {
            api.addSearchHistory(SearchHistoryEntryRequest(term = query))
        }
        response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
    }

    override suspend fun clearHistory(): Result<Unit> = suspendRunCatching {
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) { apiHolder.require(scope).clearSearchHistory() }
        response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
    }
}
