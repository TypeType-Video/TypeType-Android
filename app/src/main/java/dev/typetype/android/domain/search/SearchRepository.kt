package dev.typetype.android.domain.search

interface SearchRepository {
    suspend fun search(
        query: String,
        service: Int = 0,
        nextPage: String? = null,
        contentFilter: String? = null,
        sortFilter: String? = null,
    ): Result<SearchPage>

    suspend fun filters(service: Int = 0): Result<SearchFilters>

    suspend fun suggestions(query: String, service: Int = 0): Result<List<String>>
}
