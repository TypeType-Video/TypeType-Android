package dev.typetype.android.data.imports

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.toDomain
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.imports.ImportDocument
import dev.typetype.android.domain.imports.ImportRepository
import dev.typetype.android.domain.imports.PipePipeRestoreSummary
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
    override suspend fun restorePipePipe(
        document: ImportDocument,
    ): Result<PipePipeRestoreSummary> = runCatching {
        validatePipePipeDocument(document)
        val scope = activeAccountScope.require()
        val body = ContentUriRequestBody(
            contentResolver = context.contentResolver,
            uri = Uri.parse(document.uri),
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

internal const val MAX_PIPEPIPE_BACKUP_BYTES = 32L * 1024 * 1024
