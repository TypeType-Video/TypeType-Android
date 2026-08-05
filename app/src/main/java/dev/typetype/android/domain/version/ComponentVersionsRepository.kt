package dev.typetype.android.domain.version

interface ComponentVersionsRepository {
    suspend fun fetch(): Result<ComponentVersions>
}
