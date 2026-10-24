package dev.typetype.android.domain.version

data class ComponentVersion(
    val service: String,
    val version: String,
    val revision: String,
    val shortRevision: String,
    val buildTime: String,
)

data class ComponentVersions(
    val frontend: ComponentVersion? = null,
    val server: ComponentVersion? = null,
    val token: ComponentVersion? = null,
    val downloader: ComponentVersion? = null,
)
