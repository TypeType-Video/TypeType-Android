package dev.typetype.android.feature.settings.imports

import dev.typetype.android.domain.imports.ImportDocument
import dev.typetype.android.domain.imports.PipePipeRestoreSummary
import dev.typetype.android.domain.imports.TypeTypeBackupCategory
import dev.typetype.android.domain.imports.TypeTypeRestoreSummary

data class ImportDataState(
    val selectedCategories: Set<TypeTypeBackupCategory> = TypeTypeBackupCategory.entries.toSet(),
    val isExportingTypeType: Boolean = false,
    val typeTypeExportComplete: Boolean = false,
    val selectedTypeTypeDocument: ImportDocument? = null,
    val isRestoringTypeType: Boolean = false,
    val typeTypeSummary: TypeTypeRestoreSummary? = null,
    val selectedPipePipeDocument: ImportDocument? = null,
    val isRestoringPipePipe: Boolean = false,
    val pipePipeSummary: PipePipeRestoreSummary? = null,
    val errorKey: String? = null,
    val errorRequestId: String? = null,
) {
    val canExportTypeType: Boolean
        get() = selectedCategories.isNotEmpty() && !isExportingTypeType && !isRestoringTypeType

    val canRestoreTypeType: Boolean
        get() = selectedTypeTypeDocument != null && !isRestoringTypeType

    val canRestorePipePipe: Boolean
        get() = selectedPipePipeDocument != null && !isRestoringPipePipe
}
