package dev.typetype.android.data.imports

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.PortabilityApplyRequestDto
import dev.typetype.android.data.network.dto.PortabilityExportRequestDto
import dev.typetype.android.data.network.dto.PortabilityJobDto
import dev.typetype.android.data.network.dto.toDomain
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.imports.ImportDocument
import dev.typetype.android.domain.imports.PortabilityDuplicatePolicy
import dev.typetype.android.domain.imports.PortabilityFormat
import dev.typetype.android.domain.imports.PortabilityJob
import dev.typetype.android.domain.imports.PortabilityRepository
import dev.typetype.android.domain.imports.TypeTypeBackupCategory
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import retrofit2.Response

@Singleton
class RemotePortabilityRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
) : PortabilityRepository {
    override suspend fun formats(): Result<List<PortabilityFormat>> = request {
        val response = it.portabilityFormats()
        response.requireSuccessfulResponse()
        response.body().orEmpty().map { format -> format.toDomain() }
    }

    override suspend fun startExport(
        format: String,
        categories: Set<TypeTypeBackupCategory>,
    ): Result<PortabilityJob> = request {
        require(categories.isNotEmpty()) { PORTABILITY_NO_CATEGORIES }
        val response = it.startPortabilityExport(
            request = PortabilityExportRequestDto(
                format = format,
                categories = categories.map(TypeTypeBackupCategory::wireName),
            ),
        )
        decoded(response)
    }

    override suspend fun startImport(
        document: ImportDocument,
        format: String,
    ): Result<PortabilityJob> = request {
        validatePortabilityDocument(document)
        val response = it.startPortabilityImport(
            format = format,
            file = MultipartBody.Part.createFormData(
                name = "file",
                filename = document.displayName,
                body = ContentUriRequestBody(
                    contentResolver = context.contentResolver,
                    uri = document.uri.toUri(),
                    mediaType = document.mediaType?.toMediaType() ?: "application/octet-stream".toMediaType(),
                    knownSize = document.sizeBytes,
                    maxBytes = MAX_PORTABILITY_UPLOAD_BYTES,
                ),
            ),
        )
        decoded(response)
    }

    override suspend fun job(jobId: String): Result<PortabilityJob> = request {
        decoded(it.portabilityJob(jobId))
    }

    override suspend fun applyImport(
        jobId: String,
        categories: Set<TypeTypeBackupCategory>,
        duplicatePolicy: PortabilityDuplicatePolicy,
    ): Result<PortabilityJob> = request {
        require(categories.isNotEmpty()) { PORTABILITY_NO_CATEGORIES }
        decoded(
            it.applyPortabilityImport(
                jobId = jobId,
                request = PortabilityApplyRequestDto(
                    categories = categories.map(TypeTypeBackupCategory::wireName),
                    duplicatePolicy = duplicatePolicy.wireName,
                ),
            ),
        )
    }

    override suspend fun cancel(jobId: String): Result<PortabilityJob> = request {
        decoded(it.cancelPortabilityJob(jobId))
    }

    override suspend fun delete(jobId: String): Result<Unit> = request { api ->
        val response = api.deletePortabilityJob(jobId)
        response.requireSuccessfulResponse()
    }

    override suspend fun downloadArtifact(
        jobId: String,
        destinationUri: String,
    ): Result<Unit> = request { api ->
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) { api.portabilityArtifact(jobId) }
        val body = response.body() ?: error("The instance returned an empty export")
        response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(destinationUri.toUri(), "wt")?.use { output ->
                body.byteStream().use { input -> input.copyTo(output) }
            } ?: error("BACKUP_DESTINATION_UNAVAILABLE")
        }
        activeAccountScope.verify(scope)
    }

    override suspend fun downloadReport(
        jobId: String,
        destinationUri: String,
    ): Result<Unit> = request { api ->
        val response = withContext(Dispatchers.IO) { api.portabilityReport(jobId) }
        val payload = response.body()?.string() ?: error("The instance returned an empty report")
        response.requireSuccessfulResponse()
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(destinationUri.toUri(), "wt")
                ?.use { output -> output.write(payload.toByteArray()) }
                ?: error("BACKUP_DESTINATION_UNAVAILABLE")
        }
    }

    private suspend fun <T> request(
        block: suspend (dev.typetype.android.data.network.TypeTypeApi) -> T,
    ): Result<T> = runCatching {
        val scope = activeAccountScope.require()
        withContext(Dispatchers.IO) { block(apiHolder.require(scope)) }
            .also { activeAccountScope.verify(scope) }
    }

    private fun decoded(response: Response<PortabilityJobDto>): PortabilityJob {
        response.requireSuccessfulResponse()
        return response.body()?.toDomain()
            ?: error("The instance returned an empty portability job")
    }
}

internal const val PORTABILITY_NO_CATEGORIES = "BACKUP_NO_CATEGORIES"

internal const val MAX_PORTABILITY_UPLOAD_BYTES = 2L * 1024 * 1024 * 1024

internal fun validatePortabilityDocument(document: ImportDocument) {
    require(document.sizeBytes == null || document.sizeBytes > 0L) {
        "IMPORT_FILE_EMPTY"
    }
    require(document.sizeBytes == null || document.sizeBytes <= MAX_PORTABILITY_UPLOAD_BYTES) {
        "IMPORT_FILE_TOO_LARGE"
    }
}
