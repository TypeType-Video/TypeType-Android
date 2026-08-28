package dev.typetype.android.data.network.dto

import dev.typetype.android.domain.imports.PortabilityCapability
import dev.typetype.android.domain.imports.PortabilityDetection
import dev.typetype.android.domain.imports.PortabilityDirection
import dev.typetype.android.domain.imports.PortabilityDuplicatePolicy
import dev.typetype.android.domain.imports.PortabilityFidelity
import dev.typetype.android.domain.imports.PortabilityFormat
import dev.typetype.android.domain.imports.PortabilityIssue
import dev.typetype.android.domain.imports.PortabilityJob
import dev.typetype.android.domain.imports.PortabilityJobState
import dev.typetype.android.domain.imports.PortabilityPreview
import dev.typetype.android.domain.imports.PortabilityProgress
import dev.typetype.android.domain.imports.TypeTypeBackupCategory

internal fun PortabilityFormatDto.toDomain(): PortabilityFormat = PortabilityFormat(
    format = format,
    adapterVersion = adapterVersion,
    capabilities = capabilities.map { capability ->
        PortabilityCapability(
            category = wireCategory(capability.category),
            directions = capability.directions.mapNotNull(::direction).toSet(),
            fidelity = fidelity(capability.fidelity),
        )
    },
    defaultExtension = defaultExtension,
    contentType = contentType,
)

internal fun PortabilityJobDto.toDomain() = PortabilityJob(
    id = id,
    kind = direction(kind),
    state = state(state),
    createdAtMillis = createdAt,
    updatedAtMillis = updatedAt,
    requestId = requestId,
    preview = preview?.toDomain(),
    result = result.orEmpty().mapNotNull { (wireName, count) ->
        categories[wireName]?.let { it to count }
    }.toMap(),
    progress = progress?.let {
        PortabilityProgress(
            phase = it.phase,
            unit = it.unit,
            processed = it.processed,
            total = it.total,
        )
    },
    errorCode = errorCode,
    errorMessage = errorMessage,
)

private fun PortabilityPreviewDto.toDomain() = PortabilityPreview(
    detection = detection?.let {
        PortabilityDetection(
            format = it.format,
            formatVersion = it.formatVersion,
            adapterVersion = it.adapterVersion,
            confidence = it.confidence,
            evidence = it.evidence,
        )
    },
    counts = counts.mapNotNull { (wireName, count) ->
        categories[wireName]?.let { it to count }
    }.toMap(),
    duplicates = duplicates,
    issues = issues.map {
        PortabilityIssue(
            category = it.category?.let(categories::get),
            code = it.code,
            message = it.message,
            count = it.count,
        )
    },
)

internal fun direction(wireName: String): PortabilityDirection =
    requireNotNull(PortabilityDirection.entries.firstOrNull { it.wireName == wireName }) {
        PORTABILITY_CONTRACT_ERROR
    }

private fun state(wireName: String): PortabilityJobState =
    requireNotNull(PortabilityJobState.entries.firstOrNull { it.wireName == wireName }) {
        PORTABILITY_CONTRACT_ERROR
    }

private fun fidelity(wireName: String) = when (wireName) {
    "complete" -> PortabilityFidelity.Complete
    "partial" -> PortabilityFidelity.Partial
    else -> throw IllegalStateException(PORTABILITY_CONTRACT_ERROR)
}

private val categories = TypeTypeBackupCategory.entries.associateBy(TypeTypeBackupCategory::wireName)

private fun wireCategory(wireName: String) =
    requireNotNull(categories[wireName]) { PORTABILITY_CONTRACT_ERROR }

private const val PORTABILITY_CONTRACT_ERROR = "The instance returned an unsupported portability value"
