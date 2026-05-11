package dev.typetype.android.data.searchhistory

import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.SearchHistoryEntryRequest
import dev.typetype.android.data.network.extractServerErrorMessage
import dev.typetype.android.domain.searchhistory.SearchHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteSearchHistoryStore @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
) : SearchHistoryRepository {

    override suspend fun loadHistory(): Result<List<String>> = runCatching {
        val api = apiHolder.require()
        val response = withContext(Dispatchers.IO) { api.searchHistory() }
        if (!response.isSuccessful) error("Search history failed (HTTP ${response.code()})")
        response.body() ?: emptyList()
    }

    override suspend fun addEntry(query: String): Result<Unit> = runCatching {
        val api = apiHolder.require()
        val response = withContext(Dispatchers.IO) {
            api.addSearchHistory(SearchHistoryEntryRequest(query))
        }
        if (!response.isSuccessful) error("Add search history failed (HTTP ${response.code()})")
    }

    override suspend fun removeEntry(query: String): Result<Unit> = runCatching {
        val api = apiHolder.require()
        val response = withContext(Dispatchers.IO) { api.removeSearchHistory(query) }
        if (!response.isSuccessful) error("Remove search history failed (HTTP ${response.code()})")
    }

    override suspend fun clearHistory(): Result<Unit> = runCatching {
        val response = withContext(Dispatchers.IO) { apiHolder.require().clearSearchHistory() }
        if (!response.isSuccessful) error(extractServerErrorMessage(response))
    }
}
