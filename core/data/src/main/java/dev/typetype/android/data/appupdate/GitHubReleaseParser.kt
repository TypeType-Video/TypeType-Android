package dev.typetype.android.data.appupdate

import dev.typetype.android.domain.appupdate.AppUpdateChannel
import dev.typetype.android.domain.appupdate.AppUpdateFailure
import dev.typetype.android.domain.appupdate.AppUpdateFailureReason
import dev.typetype.android.domain.appupdate.AppUpdateRelease
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal object GitHubReleaseParser {
    private val stableVersion = Regex("""^v(\d+\.\d+\.\d+)$""")
    private val betaVersion = Regex("""^v(\d+\.\d+\.\d+-beta\.\d+)$""")
    private val betaAsset = Regex("""^dev\.typetype\.android_(\d+)\.apk$""")
    private val sha256Value = Regex("""^[a-fA-F0-9]{64}$""")

    fun selectRelease(channel: AppUpdateChannel, releases: List<GitHubReleaseDto>): GitHubReleaseDto {
        val selected = when (channel) {
            AppUpdateChannel.F_DROID -> throw AppUpdateFailure(AppUpdateFailureReason.INVALID_RELEASE)
            AppUpdateChannel.GITHUB_STABLE -> releases.firstOrNull { !it.draft && !it.prerelease }
            AppUpdateChannel.GITHUB_BETA -> releases
                .asSequence()
                .filter { !it.draft && it.prerelease }
                .maxByOrNull { it.publishedAt.orEmpty() }
        } ?: throw AppUpdateFailure(AppUpdateFailureReason.INVALID_RELEASE)
        val validTag = when (channel) {
            AppUpdateChannel.F_DROID -> false
            AppUpdateChannel.GITHUB_STABLE -> stableVersion.matches(selected.tagName)
            AppUpdateChannel.GITHUB_BETA -> betaVersion.matches(selected.tagName)
        }
        if (!validTag) throw AppUpdateFailure(AppUpdateFailureReason.INVALID_RELEASE)
        return selected
    }

    fun release(
        channel: AppUpdateChannel,
        release: GitHubReleaseDto,
        manifest: GitHubUpdateManifestDto?,
        checksum: String?,
    ): AppUpdateRelease {
        val versionName = release.tagName.removePrefix("v")
        val apk = findApkAsset(channel, release, manifest)
            ?: throw AppUpdateFailure(AppUpdateFailureReason.INVALID_RELEASE)
        validateAssetUrl(apk, release.tagName)
        if (apk.size !in 1..MAX_APK_SIZE_BYTES) {
            throw AppUpdateFailure(AppUpdateFailureReason.INVALID_RELEASE)
        }
        val digest = apk.digest
            ?.removePrefix("sha256:")
            ?.takeIf(sha256Value::matches)
            ?: checksum?.takeIf(sha256Value::matches)
            ?: manifest?.sha256?.takeIf(sha256Value::matches)
            ?: throw AppUpdateFailure(AppUpdateFailureReason.INVALID_CHECKSUM)
        if (manifest != null) {
            if (
                manifest.schemaVersion != SUPPORTED_MANIFEST_VERSION ||
                manifest.channel != channel.manifestName ||
                manifest.packageName != PACKAGE_NAME ||
                manifest.versionName != versionName ||
                manifest.versionCode <= 0 ||
                !sha256Value.matches(manifest.sha256) ||
                apk.digest?.let { it != "sha256:" + manifest.sha256 } == true
            ) {
                throw AppUpdateFailure(AppUpdateFailureReason.INVALID_RELEASE)
            }
        }
        val versionCode = manifest?.versionCode
            ?: betaAsset.matchEntire(apk.name)?.groupValues?.getOrNull(1)?.toLongOrNull()
        if (channel == AppUpdateChannel.GITHUB_BETA && versionCode == null) {
            throw AppUpdateFailure(AppUpdateFailureReason.INVALID_RELEASE)
        }
        return AppUpdateRelease(
            channel = channel,
            tagName = release.tagName,
            versionName = versionName,
            versionCode = versionCode,
            releaseNotes = release.body.orEmpty().take(MAX_RELEASE_NOTES_LENGTH),
            apkName = apk.name,
            apkUrl = apk.browserDownloadUrl,
            sha256 = digest.lowercase(),
            apkSizeBytes = apk.size,
        )
    }

    fun checksumAsset(release: GitHubReleaseDto, apkName: String): GitHubReleaseAssetDto? =
        release.assets.firstOrNull { it.name == "$apkName.sha256" }
            ?.also { validateAssetUrl(it, release.tagName) }

    fun manifestAsset(release: GitHubReleaseDto): GitHubReleaseAssetDto? =
        release.assets.firstOrNull { it.name == MANIFEST_ASSET_NAME }
            ?.also { validateAssetUrl(it, release.tagName) }

    fun findApkAsset(
        channel: AppUpdateChannel,
        release: GitHubReleaseDto,
        manifest: GitHubUpdateManifestDto?,
    ): GitHubReleaseAssetDto? = release.assets.firstOrNull { asset ->
        if (manifest != null) {
            asset.name == manifest.apkAsset
        } else {
            when (channel) {
                AppUpdateChannel.GITHUB_STABLE -> asset.name == "TypeType-" + release.tagName + ".apk"
                AppUpdateChannel.GITHUB_BETA -> betaAsset.matches(asset.name)
                AppUpdateChannel.F_DROID -> false
            }
        }
    }?.also { validateAssetUrl(it, release.tagName) }

    private fun validateAssetUrl(asset: GitHubReleaseAssetDto, tag: String) {
        val url = asset.browserDownloadUrl.toHttpUrlOrNull()
        val expectedPath = "/TypeType-Video/TypeType-Android/releases/download/" + tag + "/" + asset.name
        if (
            url?.scheme != "https" ||
            url.host != GITHUB_HOST ||
            url.encodedPath != expectedPath
        ) {
            throw AppUpdateFailure(AppUpdateFailureReason.INVALID_RELEASE)
        }
    }

    private val AppUpdateChannel.manifestName: String
        get() = when (this) {
            AppUpdateChannel.GITHUB_STABLE -> "stable"
            AppUpdateChannel.GITHUB_BETA -> "beta"
            AppUpdateChannel.F_DROID -> "fdroid"
        }

    private const val PACKAGE_NAME = "dev.typetype.android"
    private const val GITHUB_HOST = "github.com"
    const val MANIFEST_ASSET_NAME = "typetype-update.json"
    private const val SUPPORTED_MANIFEST_VERSION = 1
    private const val MAX_APK_SIZE_BYTES = 200L * 1024 * 1024
    private const val MAX_RELEASE_NOTES_LENGTH = 20_000
}
