package dev.typetype.android.data.appupdate

import dev.typetype.android.domain.appupdate.AppUpdateChannel
import dev.typetype.android.domain.appupdate.AppUpdateCheck
import dev.typetype.android.domain.appupdate.AppUpdateFailure
import dev.typetype.android.domain.appupdate.AppUpdateFailureReason
import dev.typetype.android.domain.appupdate.AppUpdateFileDownloader
import dev.typetype.android.domain.appupdate.AppUpdatePolicy
import dev.typetype.android.domain.appupdate.AppUpdateRelease
import dev.typetype.android.domain.appupdate.AppUpdateRepository
import dev.typetype.android.domain.version.AppVersion
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody

class GitHubAppUpdateRepository @Inject constructor(
    @Named("appUpdates") private val client: OkHttpClient,
    private val json: Json,
    private val downloader: AppUpdateFileDownloader,
    private val appVersion: AppVersion,
) : AppUpdateRepository {
    internal var apiBaseUrl = API_BASE
    override suspend fun check(
        channel: AppUpdateChannel,
        installedVersionName: String,
        installedVersionCode: Long,
    ): AppUpdateCheck {
        if (channel == AppUpdateChannel.F_DROID) return AppUpdateCheck.ManagedByF_Droid
        val release = fetchRelease(channel)
        val manifest = GitHubReleaseParser.manifestAsset(release)
            ?.let { asset ->
                val text = fetchText(asset.browserDownloadUrl, MAX_MANIFEST_BYTES)
                decodeJson<GitHubUpdateManifestDto>(text)
            }
        val apk = GitHubReleaseParser.findApkAsset(channel, release, manifest)
            ?: throw AppUpdateFailure(AppUpdateFailureReason.INVALID_RELEASE)
        val checksum = if (apk.digest.isNullOrBlank()) {
            GitHubReleaseParser.checksumAsset(release, apk.name)
                ?.let { fetchChecksum(it.browserDownloadUrl, apk.name) }
        } else {
            null
        }
        val appRelease = GitHubReleaseParser.release(channel, release, manifest, checksum)
        return AppUpdatePolicy.check(channel, installedVersionName, installedVersionCode, appRelease)
    }

    override suspend fun download(
        release: AppUpdateRelease,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): File = downloader.download(release, onProgress)

    private suspend fun fetchRelease(channel: AppUpdateChannel): GitHubReleaseDto {
        val path = when (channel) {
            AppUpdateChannel.F_DROID -> throw AppUpdateFailure(AppUpdateFailureReason.INVALID_RELEASE)
            AppUpdateChannel.GITHUB_STABLE -> "/releases/latest"
            AppUpdateChannel.GITHUB_BETA -> "/releases?per_page=100"
        }
        val responseText = fetchText("$apiBaseUrl$path", MAX_RELEASE_RESPONSE_BYTES)
        return try {
            val releases = if (channel == AppUpdateChannel.GITHUB_STABLE) {
                listOf(json.decodeFromString<GitHubReleaseDto>(responseText))
            } else {
                json.decodeFromString<List<GitHubReleaseDto>>(responseText)
            }
            GitHubReleaseParser.selectRelease(channel, releases)
        } catch (failure: AppUpdateFailure) {
            throw failure
        } catch (failure: SerializationException) {
            throw AppUpdateFailure(AppUpdateFailureReason.INVALID_RELEASE, failure)
        }
    }

    private suspend fun fetchChecksum(url: String, apkName: String): String {
        val checksumText = fetchText(url, MAX_CHECKSUM_BYTES).trim()
        val fields = checksumText.split(WHITESPACE, limit = 2)
        val digest = fields.firstOrNull()
            ?.takeIf(SHA_256::matches)
            ?: throw AppUpdateFailure(AppUpdateFailureReason.INVALID_CHECKSUM)
        val namedFile = fields.getOrNull(1)?.trim()?.removePrefix("*")
        if (namedFile != null && namedFile != apkName) {
            throw AppUpdateFailure(AppUpdateFailureReason.INVALID_CHECKSUM)
        }
        return digest
    }

    private suspend fun fetchText(url: String, maximumBytes: Long): String =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", GITHUB_API_VERSION)
                .header("User-Agent", "TypeType-Android/" + appVersion.name)
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw AppUpdateFailure(AppUpdateFailureReason.NETWORK)
                    }
                    readBounded(response.body, maximumBytes)
                }
            } catch (failure: AppUpdateFailure) {
                throw failure
            } catch (failure: IOException) {
                throw AppUpdateFailure(AppUpdateFailureReason.NETWORK, failure)
            }
        }

    private fun readBounded(body: ResponseBody, maximumBytes: Long): String {
        if (body.contentLength() > maximumBytes) {
            throw AppUpdateFailure(AppUpdateFailureReason.INVALID_RELEASE)
        }
        val output = ByteArrayOutputStream()
        body.byteStream().use { input ->
            val buffer = ByteArray(TEXT_BUFFER_SIZE)
            var total = 0L
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > maximumBytes) {
                    throw AppUpdateFailure(AppUpdateFailureReason.INVALID_RELEASE)
                }
                output.write(buffer, 0, count)
            }
        }
        return output.toString(Charsets.UTF_8.name())
    }

    private inline fun <reified T> decodeJson(value: String): T = try {
        json.decodeFromString<T>(value)
    } catch (failure: SerializationException) {
        throw AppUpdateFailure(AppUpdateFailureReason.INVALID_RELEASE, failure)
    }

    private companion object {
        const val API_BASE = "https://api.github.com/repos/TypeType-Video/TypeType-Android"
        const val GITHUB_API_VERSION = "2022-11-28"
        const val MAX_RELEASE_RESPONSE_BYTES = 2L * 1024 * 1024
        const val MAX_MANIFEST_BYTES = 32L * 1024
        const val MAX_CHECKSUM_BYTES = 4L * 1024
        const val TEXT_BUFFER_SIZE = 8 * 1024
        val WHITESPACE = Regex("""\s+""")
        val SHA_256 = Regex("""^[a-fA-F0-9]{64}$""")
    }
}
