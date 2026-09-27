package dev.typetype.android.feature.player

import dev.typetype.android.data.stream.StreamProvider
import dev.typetype.android.data.stream.streamProvider

internal fun supportsServerBulletComments(videoUrl: String): Boolean =
    when (videoUrl.streamProvider()) {
        StreamProvider.NicoNico, StreamProvider.BiliBili -> true
        StreamProvider.YouTube, StreamProvider.Generic -> false
    }
