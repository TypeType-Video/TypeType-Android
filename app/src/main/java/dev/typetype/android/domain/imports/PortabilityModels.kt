package dev.typetype.android.domain.imports

data class PortabilityCapability(
    val category: TypeTypeBackupCategory,
    val directions: Set<PortabilityDirection>,
    val fidelity: PortabilityFidelity,
)

data class PortabilityFormat(
    val format: String,
    val adapterVersion: Int,
    val capabilities: List<PortabilityCapability>,
    val defaultExtension: String,
    val contentType: String,
) {
    fun supports(direction: PortabilityDirection, category: TypeTypeBackupCategory) =
        capabilities.any {
            it.category == category && direction in it.directions
        }
}

enum class PortabilityDirection(val wireName: String) {
    Export("export"),
    Import("import"),
}

enum class PortabilityFidelity(val wireName: String) {
    Complete("complete"),
    Partial("partial"),
}

enum class PortabilityDuplicatePolicy(val wireName: String) {
    Skip("skip"),
    Replace("replace"),
}

enum class PortabilityJobState(val wireName: String) {
    Queued("queued"),
    Analyzing("analyzing"),
    Ready("ready"),
    Applying("applying"),
    Encoding("encoding"),
    Completed("completed"),
    Failed("failed"),
    Cancelled("cancelled"),
}

data class PortabilityDetection(
    val format: String,
    val formatVersion: String?,
    val adapterVersion: Int,
    val confidence: Double,
    val evidence: String,
)

data class PortabilityIssue(
    val category: TypeTypeBackupCategory?,
    val code: String,
    val message: String,
    val count: Int,
)

data class PortabilityPreview(
    val detection: PortabilityDetection?,
    val counts: Map<TypeTypeBackupCategory, Int>,
    val duplicates: Int,
    val issues: List<PortabilityIssue>,
)

data class PortabilityProgress(
    val phase: String?,
    val unit: String?,
    val processed: Int,
    val total: Int?,
)

data class PortabilityJob(
    val id: String,
    val kind: PortabilityDirection,
    val state: PortabilityJobState,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val requestId: String? = null,
    val preview: PortabilityPreview? = null,
    val result: Map<TypeTypeBackupCategory, Int> = emptyMap(),
    val progress: PortabilityProgress? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
) {
    val isTerminal: Boolean
        get() = state == PortabilityJobState.Ready ||
            state == PortabilityJobState.Completed ||
            state == PortabilityJobState.Failed ||
            state == PortabilityJobState.Cancelled
}
