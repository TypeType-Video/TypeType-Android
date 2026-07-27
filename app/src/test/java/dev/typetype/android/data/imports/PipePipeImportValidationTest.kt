package dev.typetype.android.data.imports

import dev.typetype.android.domain.imports.ImportDocument
import org.junit.Assert.assertThrows
import org.junit.Test

class PipePipeImportValidationTest {
    @Test
    fun acceptsZipAtTheServerLimit() {
        validatePipePipeDocument(document("backup.ZIP", MAX_PIPEPIPE_BACKUP_BYTES))
    }

    @Test
    fun rejectsAnotherExtension() {
        assertThrows(IllegalArgumentException::class.java) {
            validatePipePipeDocument(document("backup.json", 1L))
        }
    }

    @Test
    fun rejectsEmptyAndOversizedArchives() {
        assertThrows(IllegalArgumentException::class.java) {
            validatePipePipeDocument(document("backup.zip", 0L))
        }
        assertThrows(IllegalArgumentException::class.java) {
            validatePipePipeDocument(
                document("backup.zip", MAX_PIPEPIPE_BACKUP_BYTES + 1L),
            )
        }
    }

    private fun document(name: String, size: Long) = ImportDocument(
        uri = "content://documents/backup",
        displayName = name,
        sizeBytes = size,
        mediaType = "application/zip",
    )
}
