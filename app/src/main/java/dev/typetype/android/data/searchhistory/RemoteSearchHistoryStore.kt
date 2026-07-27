package dev.typetype.android.data.searchhistory

import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.SearchHistoryEntryRequest
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.searchhistory.SearchHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteSearchHistoryStore @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
) : SearchHistoryRepository {

    override suspend fun loadHistory(): Result<List<String>> = runCatching {
        val scope = activeAccountScope.require()
        val api = apiHolder.require(scope)
        val response = withContext(Dispatchers.IO) { api.searchHistory() }
        response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
        response.body() ?: emptyList()
    }

    override suspend fun addEntry(query: String): Result<Unit> = runCatching {
        val scope = activeAccountScope.require()
        val api = apiHolder.require(scope)
        val response = withContext(Dispatchers.IO) {
            api.addSearchHistory(SearchHistoryEntryRequest(query))
        }
        response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
    }

    override suspend fun removeEntry(query: String): Result<Unit> = runCatching {
        val scope = activeAccountScope.require()
        val api = apiHolder.require(scope)
        val response = withContext(Dispatchers.IO) { api.removeSearchHistory(query) }
        response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
    }

    override suspend fun clearHistory(): Result<Unit> = runCatching {
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) { apiHolder.require(scope).clearSearchHistory() }
        response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
    }
}
