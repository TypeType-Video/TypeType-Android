package dev.typetype.android.domain.search

interface SearchRepository {
    suspend fun search(
        query: String,
        service: Int = 0,
        nextPage: String? = null,
        contentFilter: String? = null,
        filters: List<String> = emptyList(),
    ): Result<SearchPage>

    suspend fun filters(service: Int = 0, contentFilter: String? = null): Result<SearchFilters>

    suspend fun suggestions(query: String, service: Int = 0): Result<List<String>>
}
