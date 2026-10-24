package dev.typetype.android.feature.settings.imports

import dev.typetype.android.domain.imports.ImportDocument
import dev.typetype.android.domain.imports.PortabilityDuplicatePolicy
import dev.typetype.android.domain.imports.PortabilityDirection
import dev.typetype.android.domain.imports.PortabilityFormat
import dev.typetype.android.domain.imports.PortabilityJob
import dev.typetype.android.domain.imports.PortabilityJobState
import dev.typetype.android.domain.imports.TypeTypeBackupCategory

data class PortabilityUiState(
    val mode: PortabilityScreenMode = PortabilityScreenMode.Import,
    val formats: List<PortabilityFormat> = emptyList(),
    val isLoadingFormats: Boolean = false,
    val selectedFormat: PortabilityFormat? = null,
    val selectedCategories: Set<TypeTypeBackupCategory> = emptySet(),
    val duplicatePolicy: PortabilityDuplicatePolicy =
        PortabilityDuplicatePolicy.Skip,
    val job: PortabilityJob? = null,
    val isStartingJob: Boolean = false,
    val isApplying: Boolean = false,
    val isCancelling: Boolean = false,
    val isSavingArtifact: Boolean = false,
    val selectedDocumentName: String? = null,
    val failureMessage: String? = null,
    val failureCode: String? = null,
    val failureRequestId: String? = null,
) {
    val availableCategories: Set<TypeTypeBackupCategory>
        get() {
            val format = selectedFormat ?: return emptySet()
            return TypeTypeBackupCategory.entries.filterTo(sortedSetOf()) { category ->
                format.supports(mode.direction, category)
            }
        }

    val previewCategories: Set<TypeTypeBackupCategory>
        get() = job?.preview?.counts?.keys.orEmpty()

    val activeCategories: Set<TypeTypeBackupCategory>
        get() = if (job?.state == PortabilityJobState.Ready) previewCategories else availableCategories
}

enum class PortabilityScreenMode(val direction: PortabilityDirection) {
    Export(PortabilityDirection.Export),
    Import(PortabilityDirection.Import),
}
