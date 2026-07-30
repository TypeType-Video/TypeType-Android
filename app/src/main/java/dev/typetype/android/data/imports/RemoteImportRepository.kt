package dev.typetype.android.data.imports

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.toDomain
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.imports.ImportDocument
import dev.typetype.android.domain.imports.ImportRepository
import dev.typetype.android.domain.imports.PipePipeRestoreSummary
import dev.typetype.android.domain.imports.TypeTypeBackupCategory
import dev.typetype.android.domain.imports.TypeTypeRestoreSummary
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody

@Singleton
class RemoteImportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
) : ImportRepository {
    override suspend fun exportTypeType(
        categories: Set<TypeTypeBackupCategory>,
        destinationUri: String,
    ): Result<Unit> = runCatching {
        require(categories.isNotEmpty()) { "BACKUP_NO_CATEGORIES" }
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) {
            apiHolder.require(scope).exportTypeType(
                categories = TypeTypeBackupCategory.entries
                    .filter(categories::contains)
                    .joinToString(",") { it.wireName },
            )
        }
        response.requireSuccessfulResponse()
        val body = response.body() ?: error("The instance returned an empty backup")
        activeAccountScope.verify(scope)
        withContext(Dispatchers.IO) {
            val destination = destinationUri.toUri()
            val output = context.contentResolver.openOutputStream(destination, "wt")
                ?: error("BACKUP_DESTINATION_UNAVAILABLE")
            body.use { backup ->
                output.use { backup.byteStream().copyTo(it) }
            }
        }
        activeAccountScope.verify(scope)
    }

    override suspend fun restoreTypeType(
        document: ImportDocument,
    ): Result<TypeTypeRestoreSummary> = runCatching {
        validateTypeTypeDocument(document)
        val scope = activeAccountScope.require()
        val body = ContentUriRequestBody(
            contentResolver = context.contentResolver,
            uri = document.uri.toUri(),
            mediaType = "application/json".toMediaType(),
            knownSize = document.sizeBytes,
            maxBytes = MAX_TYPETYPE_BACKUP_BYTES,
        )
        val part = MultipartBody.Part.createFormData(
            name = "file",
            filename = document.displayName,
            body = body,
        )
        val response = withContext(Dispatchers.IO) {
            apiHolder.require(scope).restoreTypeType(file = part)
        }
        response.requireSuccessfulResponse()
        val summary = response.body() ?: error("The instance returned an empty restore summary")
        activeAccountScope.verify(scope)
        TypeTypeRestoreSummary(
            restored = summary.restored.mapNotNull { (wireName, count) ->
                TypeTypeBackupCategory.entries
                    .firstOrNull { it.wireName == wireName }
                    ?.let { it to count }
            }.toMap(),
        )
    }

    override suspend fun restorePipePipe(
        document: ImportDocument,
    ): Result<PipePipeRestoreSummary> = runCatching {
        validatePipePipeDocument(document)
        val scope = activeAccountScope.require()
        val body = ContentUriRequestBody(
            contentResolver = context.contentResolver,
            uri = document.uri.toUri(),
            mediaType = "application/zip".toMediaType(),
            knownSize = document.sizeBytes,
            maxBytes = MAX_PIPEPIPE_BACKUP_BYTES,
        )
        val part = MultipartBody.Part.createFormData(
            name = "file",
            filename = document.displayName,
            body = body,
        )
        val response = withContext(Dispatchers.IO) {
            apiHolder.require(scope).restorePipePipe(file = part)
        }
        response.requireSuccessfulResponse()
        val result = response.body() ?: error("The instance returned an empty restore summary")
        activeAccountScope.verify(scope)
        result.toDomain()
    }
}

internal fun validatePipePipeDocument(document: ImportDocument) {
    require(document.displayName.endsWith(".zip", ignoreCase = true)) {
        "IMPORT_FILE_NOT_ZIP"
    }
    require(document.sizeBytes == null || document.sizeBytes <= MAX_PIPEPIPE_BACKUP_BYTES) {
        "IMPORT_FILE_TOO_LARGE"
    }
    require(document.sizeBytes == null || document.sizeBytes > 0L) {
        "IMPORT_FILE_EMPTY"
    }
}

internal fun validateTypeTypeDocument(document: ImportDocument) {
    require(document.displayName.endsWith(".json", ignoreCase = true)) {
        "BACKUP_FILE_NOT_JSON"
    }
    require(document.sizeBytes == null || document.sizeBytes <= MAX_TYPETYPE_BACKUP_BYTES) {
        "IMPORT_FILE_TOO_LARGE"
    }
    require(document.sizeBytes == null || document.sizeBytes > 0L) {
        "IMPORT_FILE_EMPTY"
    }
}

internal const val MAX_PIPEPIPE_BACKUP_BYTES = 32L * 1024 * 1024
internal const val MAX_TYPETYPE_BACKUP_BYTES = 128L * 1024 * 1024
