package dev.typetype.android.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import dev.typetype.android.R
import dev.typetype.android.core.ui.branding.rememberVideoBranding
import dev.typetype.android.core.ui.components.AnimatedLoader
import dev.typetype.android.core.ui.components.FullScreenLoader
import dev.typetype.android.core.ui.components.VideoMoreActionsButton
import dev.typetype.android.domain.library.HistoryItem
import dev.typetype.android.domain.library.VideoMeta
import dev.typetype.android.feature.library.components.rememberVideoMetas
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private const val LOAD_MORE_THRESHOLD = 8

@Composable
fun HistoryTab(
    pagingData: Flow<PagingData<HistoryItem>>,
    filter: String,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onPlayVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    onPlayNext: (HistoryItem) -> Unit,
    onAddToQueue: (HistoryItem) -> Unit,
    onLoadMore: () -> Unit,
) {
    val items = pagingData.collectAsLazyPagingItems()
    if (items.itemCount == 0 && isRefreshing) {
        FullScreenLoader()
        return
    }
    if (items.itemCount == 0 && !hasMore) {
        EmptyTab(emptyMessageFor(filter, stringResource(R.string.library_empty_history)))
        return
    }
    val urlsMissingInfo = (0 until items.itemCount).mapNotNull(items::peek)
        .filter { it.channelAvatarUrl.isBlank() }
        .map { it.url }
    val metas = rememberVideoMetas(urlsMissingInfo)
    val gridState = rememberLazyGridState()
    val shouldLoadMore by remember(items.itemCount, hasMore, isLoadingMore) {
        derivedStateOf {
            if (!hasMore || isLoadingMore) {
                false
            } else if (items.itemCount <= LOAD_MORE_THRESHOLD) {
                true
            } else {
                val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                lastVisible >= items.itemCount - LOAD_MORE_THRESHOLD
            }
        }
    }
    LaunchedEffect(gridState, items.itemCount, hasMore, isLoadingMore) {
        snapshotFlow { shouldLoadMore }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 400.dp),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    ) {
        items(
            count = items.itemCount,
            key = { index -> items.peek(index)?.id ?: "history-$index" },
            contentType = { "history-video" },
        ) { index ->
            items[index]?.let { item ->
                HistoryRow(
                    item = item,
                    meta = metas[item.url],
                    onClick = { onPlayVideo(item.url) },
                    onOpenChannel = onOpenChannel,
                    onPlayNext = { onPlayNext(item) },
                    onAddToQueue = { onAddToQueue(item) },
                )
            }
        }
        if (isLoadingMore) {
            item(key = "history-load-more", span = { GridItemSpan(maxLineSpan) }) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    AnimatedLoader(size = 56.dp)
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    item: HistoryItem,
    meta: VideoMeta?,
    onClick: () -> Unit,
    onOpenChannel: (String) -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
) {
    val branding = rememberVideoBranding(
        sourceUrl = item.url,
        title = item.title,
        thumbnailUrl = item.thumbnailUrl,
        durationSeconds = item.durationSeconds,
    )
    val channelUrl = item.channelUrl.takeIf { it.isNotBlank() }
        ?: meta?.channelUrl?.takeIf { it.isNotBlank() }
    val avatarUrl = item.channelAvatarUrl.takeIf { it.isNotBlank() }
        ?: meta?.channelAvatarUrl?.takeIf { it.isNotBlank() }
    val channelActionDescription = if (channelUrl == null) {
        null
    } else if (item.channelName.isBlank()) {
        stringResource(R.string.video_menu_open_channel)
    } else {
        stringResource(R.string.video_open_channel_accessibility, item.channelName)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(160.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = branding.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (item.durationSeconds > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = formatVideoDuration(item.durationSeconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = branding.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (avatarUrl != null) {
                    val avatarModifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .let {
                            if (channelUrl != null) {
                                it.clickable(role = Role.Button) { onOpenChannel(channelUrl) }
                            } else {
                                it
                            }
                        }
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = channelActionDescription,
                        contentScale = ContentScale.Crop,
                        modifier = avatarModifier,
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = item.channelName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (channelUrl != null) {
                        Modifier.clickable(role = Role.Button) { onOpenChannel(channelUrl) }
                    } else {
                        Modifier
                    },
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatDate(item.watchedAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
        HistoryQueueMenu(onPlayNext = onPlayNext, onAddToQueue = onAddToQueue)
    }
}

@Composable
private fun HistoryQueueMenu(
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        VideoMoreActionsButton(onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.video_menu_play_next)) },
                leadingIcon = { Icon(Icons.Filled.SkipNext, contentDescription = null) },
                onClick = {
                    expanded = false
                    onPlayNext()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.video_menu_add_to_queue)) },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    onAddToQueue()
                },
            )
        }
    }
}

private fun formatVideoDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))
