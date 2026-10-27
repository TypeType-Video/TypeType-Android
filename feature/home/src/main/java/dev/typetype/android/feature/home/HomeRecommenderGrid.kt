package dev.typetype.android.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.typetype.android.core.ui.components.VideoCard
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.feature.menu.VideoMenuScope

internal fun LazyGridScope.recommendedRows(
    recommender: HomeRecommenderState,
    videos: List<Video>,
    menuScope: VideoMenuScope,
    onPlayVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    onAction: (HomeAction) -> Unit,
) {
    val byUrl = videos.associateBy { it.url }
    recommender.rows.forEach { row ->
        val listed = row.videoUrls.mapNotNull(byUrl::get)
        if (listed.isEmpty()) return@forEach
        item(key = "row-${row.id}", span = { GridItemSpan(maxLineSpan) }, contentType = "recommended-row") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeRowHeader(title = row.title, reason = row.reason)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(listed, key = { it.url }, contentType = { "row-video" }) { video ->
                        RecommendedVideo(
                            video = video,
                            refusal = recommender.refused[video.url],
                            menuScope = menuScope,
                            onPlayVideo = onPlayVideo,
                            onOpenChannel = onOpenChannel,
                            onAction = onAction,
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun RecommendedVideo(
    video: Video,
    refusal: VideoRefusal?,
    menuScope: VideoMenuScope,
    onPlayVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    onAction: (HomeAction) -> Unit,
) {
    Column(modifier = Modifier.width(ROW_CARD_WIDTH)) {
        if (refusal != null) {
            RefusedVideoCard(
                refusal = refusal,
                onExplain = { reason -> onAction(HomeAction.ExplainRefusal(video, reason)) },
                onUndo = { onAction(HomeAction.UndoRefusal(video.url)) },
            )
        } else {
            VideoCard(
                video = video,
                onClick = { onPlayVideo(video.url) },
                onChannelClick = { onOpenChannel(video.uploaderUrl) },
                onMenuAction = { action -> menuScope.onAction(action, video) },
                menuItemState = menuScope.stateFor(video),
            )
            NotForMeButton(onClick = { onAction(HomeAction.NotForMe(video)) })
        }
    }
}

internal fun HomeRecommenderState.hasContinueRow(): Boolean =
    personalized && rows.any { it.reason == "continue" }

private val ROW_CARD_WIDTH = 280.dp
