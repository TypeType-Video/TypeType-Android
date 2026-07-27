package dev.typetype.android.feature.settings.imports

import dev.typetype.android.domain.imports.ImportDocument
import dev.typetype.android.domain.imports.PipePipeRestoreSummary

data class ImportDataState(
    val selectedDocument: ImportDocument? = null,
    val isRestoring: Boolean = false,
    val summary: PipePipeRestoreSummary? = null,
    val errorKey: String? = null,
    val errorRequestId: String? = null,
) {
    val canRestore: Boolean
        get() = selectedDocument != null && !isRestoring
}
