package dev.typetype.android.services.push

import android.content.Context
import android.graphics.Bitmap
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import androidx.core.graphics.blue
import androidx.core.graphics.get
import androidx.core.graphics.green
import androidx.core.graphics.red
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.core.error.suspendRunCatching
import dev.typetype.android.core.url.buildImageUrl
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class NotificationThumbnails @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverRepository: ServerRepository,
) {
    suspend fun load(scope: AccountScope, thumbnailUrl: String): Bitmap? {
        if (thumbnailUrl.isBlank()) return null
        val baseUrl = suspendRunCatching { serverRepository.getServer(scope.serverId)?.baseUrl }.getOrNull()
        val request = ImageRequest.Builder(context)
            .data(buildImageUrl(baseUrl, thumbnailUrl))
            .size(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
            .allowHardware(false)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .build()
        val bitmap = withTimeoutOrNull(LOAD_TIMEOUT_MS) {
            (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image?.toBitmap()
        }
        return bitmap?.takeUnless { it.isProcessingPlaceholder() }
    }

    private companion object {
        const val THUMBNAIL_WIDTH = 640
        const val THUMBNAIL_HEIGHT = 360
        const val LOAD_TIMEOUT_MS = 8_000L
    }
}

private fun Bitmap.isProcessingPlaceholder(): Boolean {
    val columns = 16
    val rows = 9
    for (row in 0 until rows) {
        for (column in 0 until columns) {
            val pixel = this[(column * 2 + 1) * width / (columns * 2), (row * 2 + 1) * height / (rows * 2)]
            val chroma = maxOf(pixel.red, pixel.green, pixel.blue) - minOf(pixel.red, pixel.green, pixel.blue)
            if (chroma > GREY_TOLERANCE) return false
        }
    }
    return true
}

private const val GREY_TOLERANCE = 12

internal fun youtubeThumbnailUrl(serviceId: Int, videoId: String): String =
    if (serviceId == NotificationPlatform.YouTube.serviceId && videoId.isNotBlank()) {
        "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
    } else {
        ""
    }
