package dev.typetype.android.data.branding

import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.DeArrowDto
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.branding.DeArrowItem
import dev.typetype.android.domain.branding.DeArrowRepository
import dev.typetype.android.domain.branding.DeArrowThumbnailCandidate
import dev.typetype.android.domain.branding.DeArrowTitleCandidate
import dev.typetype.android.domain.server.ServerRepository
import java.util.LinkedHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Singleton
class RemoteDeArrowRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
    private val serverRepository: ServerRepository,
) : DeArrowRepository {
    private val cacheMutex = Mutex()
    private val cache = LinkedHashMap<CacheKey, CachedItem>(16, 0.75f, true)

    override suspend fun load(sourceUrl: String, durationSeconds: Long): Result<DeArrowItem?> =
        captureResult {
            val videoId = youtubeVideoId(sourceUrl) ?: return@captureResult null
            val scope = activeAccountScope.require()
            val key = CacheKey(scope, videoId, durationSeconds.coerceAtLeast(0))
            cached(key)?.let { return@captureResult it }
            val response = withContext(Dispatchers.IO) {
                apiHolder.require(scope).deArrow(videoId)
            }
            response.requireSuccessfulResponse()
            val body = response.body() ?: error("The instance returned empty DeArrow metadata")
            val baseUrl = serverRepository.getServer(scope.serverId)?.baseUrl
                ?: error("Instance not found")
            val item = body.toDomain(baseUrl, durationSeconds)
            activeAccountScope.verify(scope)
            cacheMutex.withLock {
                cache[key] = CachedItem(item, System.currentTimeMillis())
                while (cache.size > MAX_CACHE_ITEMS) cache.remove(cache.keys.first())
            }
            item
        }

    private suspend fun cached(key: CacheKey): DeArrowItem? = cacheMutex.withLock {
        val cached = cache[key] ?: return@withLock null
        cached.item.takeIf { System.currentTimeMillis() - cached.storedAt < CACHE_TTL_MILLIS }
            ?: run {
                cache.remove(key)
                null
            }
    }

    private data class CacheKey(
        val scope: AccountScope,
        val videoId: String,
        val durationSeconds: Long,
    )

    private data class CachedItem(val item: DeArrowItem, val storedAt: Long)

    private companion object {
        const val MAX_CACHE_ITEMS = 128
        const val CACHE_TTL_MILLIS = 24 * 60 * 60 * 1_000L
    }
}

internal fun DeArrowDto.toDomain(baseUrl: String, fallbackDurationSeconds: Long): DeArrowItem {
    val duration = videoDuration?.takeIf { it > 0 }
        ?: fallbackDurationSeconds.toDouble().takeIf { it > 0 }
    val neutralUrl = randomTime?.takeIf { it > 0 }
        ?.let { time -> duration?.let { deArrowThumbnailUrl(baseUrl, videoId, time * it) } }
    return DeArrowItem(
        videoId = videoId,
        legacyTitle = title,
        legacyThumbnailUrl = resolveDeArrowUrl(baseUrl, thumbnailUrl),
        titles = titles?.map {
            DeArrowTitleCandidate(it.title, it.original, it.votes, it.locked)
        },
        thumbnails = thumbnails?.map {
            DeArrowThumbnailCandidate(
                resolveDeArrowUrl(baseUrl, it.thumbnailUrl),
                it.original,
                it.votes,
                it.locked,
            )
        },
        neutralThumbnailUrl = neutralUrl,
    )
}

internal fun youtubeVideoId(value: String): String? {
    val trimmed = value.trim()
    if (YOUTUBE_VIDEO_ID.matches(trimmed)) return trimmed
    val url = trimmed.toHttpUrlOrNull() ?: return null
    val host = url.host.lowercase()
    val candidate = if (host == "youtu.be") {
        url.pathSegments.firstOrNull()
    } else if (host == "youtube.com" || host.endsWith(".youtube.com")) {
        url.queryParameter("v") ?: youtubeVideoIdFromPath(url)
    } else {
        null
    }
    return candidate?.takeIf(YOUTUBE_VIDEO_ID::matches)
}

private fun youtubeVideoIdFromPath(url: HttpUrl): String? {
    val segments = url.pathSegments.filter(String::isNotBlank)
    return if (segments.firstOrNull() in setOf("shorts", "embed", "live")) {
        segments.getOrNull(1)
    } else {
        segments.firstOrNull()
    }
}

private fun deArrowThumbnailUrl(baseUrl: String, videoId: String, time: Double): String? =
    baseUrl.toHttpUrlOrNull()?.newBuilder()
        ?.addPathSegment("dearrow")
        ?.addPathSegment("thumbnail")
        ?.addQueryParameter("videoId", videoId)
        ?.addQueryParameter("time", time.toString())
        ?.build()
        ?.toString()

private fun resolveDeArrowUrl(baseUrl: String, value: String?): String? {
    val source = value?.takeIf(String::isNotBlank) ?: return null
    val server = baseUrl.toHttpUrlOrNull() ?: return null
    val resolved = source.toHttpUrlOrNull() ?: server.resolve(source.removePrefix("/")) ?: return null
    return resolved.takeIf {
        it.scheme == server.scheme && it.host == server.host && it.port == server.port
    }?.toString()
}

private suspend fun <T> captureResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Throwable) {
    Result.failure(failure)
}

private val YOUTUBE_VIDEO_ID = Regex("[A-Za-z0-9_-]{11}")
