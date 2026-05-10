package dev.typetype.android.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.core.ui.components.AnimatedError
import dev.typetype.android.core.ui.components.FullScreenLoader
import dev.typetype.android.core.ui.components.HorizontalVideoCard
import dev.typetype.android.core.ui.components.SectionHeader
import dev.typetype.android.core.ui.components.VideoCard
import dev.typetype.android.feature.menu.rememberVideoMenuScope

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenChannel: (channelUrl: String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onPlayVideo = onPlayVideo,
        onOpenChannel = onOpenChannel,
        onRetry = { viewModel.onAction(HomeAction.OnRefresh) },
    )
}

@Composable
fun HomeScreen(
    state: HomeState,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenChannel: (channelUrl: String) -> Unit,
    onRetry: () -> Unit,
) {
    val menuScope = rememberVideoMenuScope(onOpenChannel = onOpenChannel)
    val visibleTop = state.topSectionVideos.filterNot(menuScope::isHidden)
    val visibleRecommendations = state.recommendations.filterNot(menuScope::isHidden)
    val nothingToShow = visibleTop.isEmpty() && visibleRecommendations.isEmpty()

    when {
        state.isLoading && nothingToShow -> FullScreenLoader()
        nothingToShow && state.recommendationsError != null && state.topSectionError != null -> {
            AnimatedError(message = state.recommendationsError, onRetry = onRetry)
        }
        nothingToShow -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Nothing here yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                if (visibleTop.isNotEmpty()) {
                    item {
                        val title = when (state.topSectionKind) {
                            TopSectionKind.Subscriptions -> "From your subscriptions"
                            TopSectionKind.Trending -> "Trending"
                        }
                        SectionHeader(text = title, modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(Modifier.height(10.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(visibleTop, key = { "top-${it.id}" }) { video ->
                                HorizontalVideoCard(
                                    video = video,
                                    onClick = { onPlayVideo(video.url) },
                                    onChannelClick = { onOpenChannel(video.uploaderUrl) },
                                    onMenuAction = { action -> menuScope.onAction(action, video) },
                                    menuItemState = menuScope.stateFor(video),
                                )
                            }
                        }
                        Spacer(Modifier.height(28.dp))
                    }
                }
                if (visibleRecommendations.isNotEmpty()) {
                    item {
                        SectionHeader(
                            text = "Recommended",
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    items(visibleRecommendations, key = { "rec-${it.id}" }) { video ->
                        VideoCard(
                            video = video,
                            onClick = { onPlayVideo(video.url) },
                            onChannelClick = { onOpenChannel(video.uploaderUrl) },
                            onMenuAction = { action -> menuScope.onAction(action, video) },
                            menuItemState = menuScope.stateFor(video),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                } else if (state.recommendationsError != null) {
                    item {
                        SectionErrorBanner(
                            sectionLabel = "Recommended",
                            message = state.recommendationsError,
                            onRetry = onRetry,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionErrorBanner(
    sectionLabel: String,
    message: String,
    onRetry: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        SectionHeader(text = sectionLabel)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.height(8.dp))
                RetryPill(onRetry = onRetry)
            }
        }
    }
}

@Composable
private fun RetryPill(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onRetry)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = "Retry",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}
