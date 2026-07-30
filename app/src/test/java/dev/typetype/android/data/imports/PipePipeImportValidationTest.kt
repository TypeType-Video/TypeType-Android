package dev.typetype.android.data.imports

import dev.typetype.android.domain.imports.ImportDocument
import dev.typetype.android.domain.imports.TypeTypeBackupCategory
import org.junit.Assert.assertEquals
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

    @Test
    fun acceptsTypeTypeJsonAtTheServerLimit() {
        validateTypeTypeDocument(document("typetype-backup.JSON", MAX_TYPETYPE_BACKUP_BYTES))
    }

    @Test
    fun rejectsInvalidTypeTypeBackups() {
        assertThrows(IllegalArgumentException::class.java) {
            validateTypeTypeDocument(document("typetype-backup.zip", 1L))
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateTypeTypeDocument(document("typetype-backup.json", 0L))
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateTypeTypeDocument(
                document("typetype-backup.json", MAX_TYPETYPE_BACKUP_BYTES + 1L),
            )
        }
    }

    @Test
    fun exposesEveryServerBackupCategory() {
        assertEquals(
            listOf(
                "subscriptions",
                "history",
                "playlists",
                "watchLater",
                "favorites",
                "progress",
                "searchHistory",
                "savedPlaylists",
                "settings",
                "contentFilters",
            ),
            TypeTypeBackupCategory.entries.map(TypeTypeBackupCategory::wireName),
        )
    }

    private fun document(name: String, size: Long) = ImportDocument(
        uri = "content://documents/backup",
        displayName = name,
        sizeBytes = size,
        mediaType = "application/zip",
    )
}
