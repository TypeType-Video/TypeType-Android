package dev.typetype.android.domain.searchhistory

interface SearchHistoryRepository {
    suspend fun loadHistory(): Result<List<String>>
    suspend fun addEntry(query: String): Result<Unit>
    suspend fun removeEntry(query: String): Result<Unit>
}
