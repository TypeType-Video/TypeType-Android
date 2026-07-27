package dev.typetype.android.data.stream

import dev.typetype.android.data.network.TypeTypeMediaApi
import dev.typetype.android.data.network.dto.StreamResponse
import dev.typetype.android.domain.stream.isServerSabrAudioFormat
import dev.typetype.android.domain.stream.isServerSabrVideoFormat
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.Response

internal suspend fun TypeTypeMediaApi.loadStreamResponse(videoUrl: String): Response<StreamResponse> {
    val provider = videoUrl.streamProvider()
    val response = when (provider) {
        StreamProvider.YouTube -> youtubeSabrStreams(videoUrl)
        StreamProvider.NicoNico -> fallbackToGeneric(nicoNicoStreams(videoUrl), videoUrl)
        StreamProvider.BiliBili -> fallbackToGeneric(biliBiliStreams(videoUrl), videoUrl)
        StreamProvider.Generic -> streams(videoUrl)
    }
    if (provider == StreamProvider.YouTube) {
        response.rejectSabrRedirect("SABR stream discovery attempted to redirect")
    }
    return response
}

internal suspend fun TypeTypeMediaApi.loadYouTubeSabrBootstrapResponse(
    videoUrl: String,
): Response<StreamResponse> {
    val response = youtubeSabrBootstrap(videoUrl)
    response.rejectSabrRedirect("SABR stream bootstrap attempted to redirect")
    return response
}

private suspend fun TypeTypeMediaApi.fallbackToGeneric(
    response: Response<StreamResponse>,
    videoUrl: String,
): Response<StreamResponse> = if (response.canFallbackToGeneric()) streams(videoUrl) else response

private fun Response<StreamResponse>.canFallbackToGeneric(): Boolean =
    code() == 404 || code() == 405 || code() == 501

internal fun StreamResponse.hasPlayableSabrContract(baseUrl: String? = null): Boolean {
    val playableVideoItags = (videoStreams + videoOnlyStreams).filter {
        it.deliveryMethod == SABR_DELIVERY_METHOD && it.itag > 0 &&
            it.manifestUrl.isAllowedSabrManifest(baseUrl) && isServerSabrVideoFormat(it.codec)
    }.mapTo(mutableSetOf()) { it.itag }
    return playableVideoItags.isNotEmpty() && audioStreams.any {
        it.deliveryMethod == SABR_DELIVERY_METHOD && it.itag > 0 &&
            it.itag !in playableVideoItags && it.manifestUrl.isAllowedSabrManifest(baseUrl) &&
            isServerSabrAudioFormat(it.mimeType, it.codec)
    }
}

private fun String?.isAllowedSabrManifest(baseUrl: String?): Boolean =
    !isNullOrBlank() && (baseUrl == null || resolveServerUrl(baseUrl, this) != null)

internal fun String.streamProvider(): StreamProvider {
    val host = toHttpUrlOrNull()?.host?.lowercase().orEmpty()
    return when {
        host == "youtu.be" || host == "youtube.com" || host.endsWith(".youtube.com") ||
            host == "youtube-nocookie.com" || host.endsWith(".youtube-nocookie.com") -> StreamProvider.YouTube
        host == "nicovideo.jp" || host.endsWith(".nicovideo.jp") || host == "nico.ms" -> StreamProvider.NicoNico
        host == "bilibili.com" || host.endsWith(".bilibili.com") || host == "b23.tv" -> StreamProvider.BiliBili
        else -> StreamProvider.Generic
    }
}

internal enum class StreamProvider { YouTube, NicoNico, BiliBili, Generic }

private const val SABR_DELIVERY_METHOD = "sabr"
