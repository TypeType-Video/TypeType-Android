package dev.typetype.android.domain.imports

interface PortabilityRepository {
    suspend fun formats(): Result<List<PortabilityFormat>>

    suspend fun startExport(
        format: String,
        categories: Set<TypeTypeBackupCategory>,
    ): Result<PortabilityJob>

    suspend fun startImport(document: ImportDocument, format: String): Result<PortabilityJob>

    suspend fun job(jobId: String): Result<PortabilityJob>

    suspend fun applyImport(
        jobId: String,
        categories: Set<TypeTypeBackupCategory>,
        duplicatePolicy: PortabilityDuplicatePolicy,
    ): Result<PortabilityJob>

    suspend fun cancel(jobId: String): Result<PortabilityJob>

    suspend fun delete(jobId: String): Result<Unit>

    suspend fun downloadArtifact(jobId: String, destinationUri: String): Result<Unit>

    suspend fun downloadReport(jobId: String, destinationUri: String): Result<Unit>
}
