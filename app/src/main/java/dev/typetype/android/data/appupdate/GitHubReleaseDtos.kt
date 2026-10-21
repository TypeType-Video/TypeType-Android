package dev.typetype.android.data.appupdate

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    @SerialName("published_at") val publishedAt: String? = null,
    val body: String? = null,
    val assets: List<GitHubReleaseAssetDto> = emptyList(),
)

@Serializable
internal data class GitHubReleaseAssetDto(
    val name: String,
    val size: Long = 0,
    val digest: String? = null,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
)

@Serializable
internal data class GitHubUpdateManifestDto(
    val schemaVersion: Int,
    val channel: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val apkAsset: String,
    val sha256: String,
)
