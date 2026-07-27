package dev.typetype.android.domain.imports

interface ImportRepository {
    suspend fun restorePipePipe(document: ImportDocument): Result<PipePipeRestoreSummary>
}
