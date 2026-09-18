package video.typetype.tv.data

import video.typetype.sdk.core.ServiceId
import video.typetype.sdk.core.StreamDetails
import video.typetype.sdk.core.TypeTypeResult

internal suspend fun TvViewModel.loadStreamDetails(
    videoUrl: String,
    service: ServiceId,
): TypeTypeResult<StreamDetails> = if (service == ServiceId.YOUTUBE) {
    client.catalog.streamBootstrap(videoUrl)
} else {
    client.catalog.stream(videoUrl, service)
}
