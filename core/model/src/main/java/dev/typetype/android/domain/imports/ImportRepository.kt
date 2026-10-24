package dev.typetype.android.domain.imports

interface ImportRepository {
    suspend fun exportTypeType(
        categories: Set<TypeTypeBackupCategory>,
        destinationUri: String,
    ): Result<Unit>

    suspend fun restoreTypeType(document: ImportDocument): Result<TypeTypeRestoreSummary>

    suspend fun restorePipePipe(document: ImportDocument): Result<PipePipeRestoreSummary>
}
