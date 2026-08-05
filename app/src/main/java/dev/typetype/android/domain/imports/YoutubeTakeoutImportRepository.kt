package dev.typetype.android.domain.imports

import kotlinx.coroutines.flow.Flow

interface YoutubeTakeoutImportRepository {
    fun observeImports(): Flow<List<YoutubeTakeoutImportItem>>

    suspend fun enqueue(documents: List<ImportDocument>): Result<Int>

    suspend fun retry(requestId: String): Result<Unit>

    suspend fun cancel(requestId: String): Result<Unit>

    suspend fun remove(requestId: String): Result<Unit>

    suspend fun acknowledgeCollectionRefresh(requestId: String): Result<Unit>
}
