package dev.typetype.android.benchmark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.typetype.android.core.ui.components.LocalAnimatedStatePlayback
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.feature.shorts.ShortsScreen
import dev.typetype.android.feature.shorts.ShortsState

class ShortsBenchmarkActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalAnimatedStatePlayback provides false) {
                TypeTypeTheme {
                    ShortsScreen(
                        state = ShortsState(videos = VIDEOS, isLoading = false),
                        onNavigateBack = {},
                        onPlayVideo = {},
                        onOpenChannel = {},
                        onRefresh = {},
                        onLoadMore = {},
                        embeddedPlaybackEnabled = true,
                        playbackReady = true,
                        embeddedPlayback = { _, _ ->
                            Box(Modifier.fillMaxSize().background(Color.Black))
                        },
                    )
                }
            }
        }
    }

    private companion object {
        val VIDEOS = List(12) { index ->
            Video(
                id = "short-$index",
                url = "https://video/short-$index",
                title = "Short $index",
                thumbnailUrl = "",
                uploaderName = "Channel $index",
                uploaderUrl = "https://channel/$index",
                uploaderAvatarUrl = "",
                uploaderVerified = index % 2 == 0,
                durationSeconds = 30L,
                isLive = false,
                viewCount = index.toLong(),
                uploadedAtMillis = index.toLong(),
                isShortFormContent = true,
                shortDescription = "Description $index",
            )
        }
    }
}
