package dev.typetype.android.feature.settings.imports

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.domain.imports.ImportDocument
import javax.inject.Inject

class ImportDocumentReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun read(uri: Uri): ImportDocument {
        val metadata = context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) null else {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                DocumentMetadata(
                    name = nameIndex.takeIf { it >= 0 }?.let(cursor::getString),
                    size = sizeIndex.takeIf { it >= 0 && !cursor.isNull(it) }?.let(cursor::getLong),
                )
            }
        }
        return ImportDocument(
            uri = uri.toString(),
            displayName = metadata?.name?.takeIf(String::isNotBlank) ?: "backup.zip",
            sizeBytes = metadata?.size,
            mediaType = context.contentResolver.getType(uri),
        )
    }

    private data class DocumentMetadata(
        val name: String?,
        val size: Long?,
    )
}
