package dev.typetype.android.feature.settings.imports

import dev.typetype.android.domain.imports.YoutubeTakeoutImportItem

data class YoutubeTakeoutImportState(
    val items: List<YoutubeTakeoutImportItem> = emptyList(),
    val isReadingDocuments: Boolean = false,
    val errorKey: String? = null,
    val errorRequestId: String? = null,
)
