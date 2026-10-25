package dev.typetype.android.data.appupdate

import dev.typetype.android.domain.appupdate.AppUpdateChannel
import dev.typetype.android.domain.appupdate.AppUpdateCheck
import dev.typetype.android.domain.appupdate.AppUpdateFailure
import dev.typetype.android.domain.appupdate.AppUpdateFailureReason
import dev.typetype.android.domain.appupdate.AppUpdateFileDownloader
import dev.typetype.android.domain.appupdate.AppUpdateRelease
import dev.typetype.android.domain.version.AppVersion
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GitHubAppUpdateRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: GitHubAppUpdateRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        repository = GitHubAppUpdateRepository(
            client = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true },
            downloader = UnusedDownloader,
            appVersion = AppVersion("1.0.0", 10000),
        ).apply {
            apiBaseUrl = server.url("/repos/TypeType-Video/TypeType-Android").toString().trimEnd('/')
        }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun stableCheckReadsLatestReleaseAndIgnoresEqualVersion() = runBlocking {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(stableRelease()))

        val result = repository.check(AppUpdateChannel.GITHUB_STABLE, "1.10.0", 11002)
        val request = server.takeRequest()

        assertEquals("/repos/TypeType-Video/TypeType-Android/releases/latest", request.path)
        assertTrue(result is AppUpdateCheck.UpToDate)
    }

    @Test
    fun stableCheckExposesReleaseDetails() = runBlocking {
        server.enqueue(
            MockResponse().setHeader("Content-Type", "application/json").setBody(
                stableRelease(version = "1.11.0", tag = "v1.11.0"),
            ),
        )

        val result = repository.check(AppUpdateChannel.GITHUB_STABLE, "1.10.0", 11002)
        val release = (result as AppUpdateCheck.Available).release

        assertEquals("v1.11.0", release.tagName)
        assertEquals("1.11.0", release.versionName)
        assertNull(release.versionCode)
        assertEquals("TypeType-v1.11.0.apk", release.apkName)
        assertEquals(HASH, release.sha256)
        assertEquals("Update notes", release.releaseNotes)
    }

    @Test
    fun betaCheckSelectsNewestPrereleaseAndReadsItsCode() = runBlocking {
        val releases = "[" +
            betaRelease("1.10.0-beta.3", 11003, "2026-10-20T10:00:00Z") + "," +
            stableRelease() + "," +
            betaRelease("1.10.0-beta.4", 11004, "2026-10-21T10:00:00Z") +
            "]"
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(releases))

        val result = repository.check(AppUpdateChannel.GITHUB_BETA, "1.10.0", 11002)
        val request = server.takeRequest()
        val release = (result as AppUpdateCheck.Available).release

        assertEquals("/repos/TypeType-Video/TypeType-Android/releases?per_page=100", request.path)
        assertEquals("v1.10.0-beta.4", release.tagName)
        assertEquals(11004L, release.versionCode)
        assertEquals(AppUpdateChannel.GITHUB_BETA, release.channel)
    }

    @Test
    fun stableDoesNotOfferSameVersionAsDowngradeFromBeta() = runBlocking {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(stableRelease()))

        val result = repository.check(AppUpdateChannel.GITHUB_STABLE, "1.10.0-beta.4", 11004)

        assertTrue(result is AppUpdateCheck.NewerBuildInstalled)
    }

    @Test
    fun stableReportsOlderReleaseWhenInstalledBetaHasNewerPatch() = runBlocking {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(stableRelease()))

        val result = repository.check(AppUpdateChannel.GITHUB_STABLE, "1.10.1-beta.1", 11005)

        assertTrue(result is AppUpdateCheck.NewerBuildInstalled)
    }

    @Test
    fun rejectsReleaseAssetsOutsideOfficialGitHubPath() = runBlocking {
        val body = stableRelease().replace(
            "https://github.com/TypeType-Video/TypeType-Android/releases/download/",
            "https://downloads.example/releases/download/",
        )
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(body))

        val failure = runCatching {
            repository.check(AppUpdateChannel.GITHUB_STABLE, "1.10.0", 11002)
        }.exceptionOrNull()

        assertTrue(failure is AppUpdateFailure)
        assertEquals(AppUpdateFailureReason.INVALID_RELEASE, (failure as AppUpdateFailure).reason)
    }

    @Test
    fun releaseManifestMustMatchSelectedChannelAndArtifact() {
        val releaseDto = Json.decodeFromString<GitHubReleaseDto>(
            stableRelease(version = "1.11.0", tag = "v1.11.0"),
        )
        val release = GitHubReleaseParser.selectRelease(AppUpdateChannel.GITHUB_STABLE, listOf(releaseDto))
        val manifest = GitHubUpdateManifestDto(
            schemaVersion = 1,
            channel = "stable",
            packageName = "dev.typetype.android",
            versionName = "1.11.0",
            versionCode = 11100,
            apkAsset = "TypeType-v1.11.0.apk",
            sha256 = HASH,
        )

        val parsed = GitHubReleaseParser.release(
            AppUpdateChannel.GITHUB_STABLE,
            release,
            manifest,
            checksum = null,
        )
        assertEquals(11100L, parsed.versionCode)
        assertEquals(HASH, parsed.sha256)
    }

    private fun stableRelease(
        version: String = "1.10.0",
        tag: String = "v1.10.0",
    ): String = """
        {
          "tag_name":"$tag",
          "draft":false,
          "prerelease":false,
          "body":"Update notes",
          "assets":[{
            "name":"TypeType-$tag.apk",
            "size":128,
            "digest":"sha256:$HASH",
            "browser_download_url":"https://github.com/TypeType-Video/TypeType-Android/releases/download/$tag/TypeType-$tag.apk"
          }]
        }
    """.trimIndent()

    private fun betaRelease(version: String, versionCode: Int, publishedAt: String): String {
        val tag = "v$version"
        val name = "dev.typetype.android_$versionCode.apk"
        return """
            {
              "tag_name":"$tag",
              "draft":false,
              "prerelease":true,
              "published_at":"$publishedAt",
              "assets":[{
                "name":"$name",
                "size":128,
                "digest":"sha256:$HASH",
                "browser_download_url":"https://github.com/TypeType-Video/TypeType-Android/releases/download/$tag/$name"
              }]
            }
        """.trimIndent()
    }

    private object UnusedDownloader : AppUpdateFileDownloader {
        override suspend fun download(
            release: AppUpdateRelease,
            onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
        ): File = error("The update checker does not download APKs")
    }

    private companion object {
        const val HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    }
}
