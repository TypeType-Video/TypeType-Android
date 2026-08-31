package video.typetype.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import video.typetype.sdk.core.Channel
import video.typetype.sdk.core.Podcast
import video.typetype.sdk.core.PodcastPage
import video.typetype.sdk.core.PlaylistPage
import video.typetype.sdk.core.Playlist
import video.typetype.sdk.core.PublicPlaylist
import video.typetype.sdk.core.Video

@Composable
public fun ChannelScreen(
    channel: Channel,
    podcasts: PodcastPage?,
    playlists: PlaylistPage?,
    isSubscribed: Boolean,
    isActionInProgress: Boolean,
    isLoadingMore: Boolean,
    errorMessage: String?,
    onOpenVideo: (Video) -> Unit,
    onOpenPodcast: (Podcast) -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    onToggleSubscription: () -> Unit,
    onLoadMore: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var focusedItem by rememberSaveable(channel.url) { mutableStateOf<String?>(null) }
    val actionFocus = remember(channel.url) { FocusRequester() }
    val listState = rememberLazyListState()
    AutoLoadMore(listState, channel.nextPage != null || playlists?.nextPage != null, isLoadingMore, onLoadMore)
    LaunchedEffect(channel.url) { actionFocus.requestFocus() }
    val hasContent = channel.videos.isNotEmpty() || podcasts?.episodes?.isNotEmpty() == true ||
        podcasts?.podcasts?.isNotEmpty() == true || playlists?.playlists?.isNotEmpty() == true
    CollectionBackdrop(channel.bannerUrl ?: channel.videos.firstOrNull()?.thumbnailUrl ?: channel.avatarUrl) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().restoreFocusWhen(true),
            state = listState,
            contentPadding = PaddingValues(bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item {
                CollectionHero(
                    eyebrow = "CHANNEL",
                    title = channel.name,
                    metadata = channel.subscriberCount.takeIf { it >= 0L }?.let(::formatSubscriberCount).orEmpty(),
                    description = channel.description,
                    avatarUrl = channel.avatarUrl,
                ) {
                    Button(
                        modifier = Modifier.focusRequester(actionFocus),
                        onClick = onToggleSubscription,
                        enabled = !isActionInProgress,
                    ) { Text(if (isSubscribed) "Subscribed" else "Subscribe") }
                }
            }
            if (channel.videos.isNotEmpty()) item {
                VideoRow(
                    "Latest videos", channel.videos, onOpenVideo,
                    restoreFocusKey = focusedItem,
                    focusActive = true,
                    onFocused = { focusedItem = it },
                )
            }
            podcasts?.episodes?.takeIf { it.isNotEmpty() }?.let { episodes ->
                item {
                    VideoRow(
                        "Podcast episodes", episodes, onOpenVideo,
                        restoreFocusKey = focusedItem,
                        focusActive = true,
                        onFocused = { focusedItem = it },
                    )
                }
            }
            podcasts?.podcasts?.takeIf { it.isNotEmpty() }?.let { values ->
                item { PodcastRow(values, onOpenPodcast, focusedItem) { focusedItem = it } }
            }
            playlists?.playlists?.takeIf { it.isNotEmpty() }?.let { values ->
                item { ChannelPlaylistRow(values, onOpenPlaylist) }
            }
            if (!hasContent && errorMessage == null) item {
                CollectionEmptyState(
                    title = "Nothing published here yet",
                    message = "This channel did not return any videos, podcasts or playlists.",
                )
            }
            errorMessage?.let { item { CollectionError(it) } }
        }
    }
}

@Composable
public fun PlaylistScreen(
    playlist: PublicPlaylist,
    isAuthenticated: Boolean,
    isSaved: Boolean,
    isActionInProgress: Boolean,
    isLoadingMore: Boolean,
    errorMessage: String?,
    onToggleSaved: () -> Unit,
    onOpenVideo: (Video) -> Unit,
    onLoadMore: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var focusedItem by rememberSaveable(playlist.playlist.url) {
        mutableStateOf(playlist.videos.firstOrNull()?.let { videoFocusKey("Videos", it) })
    }
    val listState = rememberLazyListState()
    AutoLoadMore(listState, playlist.nextPage != null, isLoadingMore, onLoadMore)
    CollectionBackdrop(playlist.videos.firstOrNull()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().restoreFocusWhen(true),
            state = listState,
            contentPadding = PaddingValues(bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item {
                CollectionHero(
                    eyebrow = "PLAYLIST",
                    title = playlist.playlist.title,
                    metadata = collectionMetadata(
                        count = playlist.playlist.streamCount,
                        singular = "video",
                        plural = "videos",
                        owner = playlist.playlist.uploaderName,
                    ),
                    compact = true,
                ) {
                    if (isAuthenticated) {
                        Button(onClick = onToggleSaved, enabled = !isActionInProgress) {
                            Text(if (isSaved) "Saved" else "Save playlist")
                        }
                    }
                }
            }
            item {
                VideoRow(
                    "Videos", playlist.videos, onOpenVideo,
                    restoreFocusKey = focusedItem,
                    focusActive = true,
                    onFocused = { focusedItem = it },
                )
            }
            errorMessage?.let { item { CollectionError(it) } }
        }
    }
}

@Composable
internal fun CollectionBackdrop(video: Video?, content: @Composable () -> Unit) {
    CollectionBackdrop(video?.thumbnailUrl, content)
}

@Composable
private fun CollectionBackdrop(imageUrl: String?, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        CinematicBackdrop(imageUrl, Modifier.fillMaxSize())
        content()
    }
}

@Composable
internal fun CollectionHero(
    eyebrow: String,
    title: String,
    metadata: String,
    description: String = "",
    compact: Boolean = false,
    avatarUrl: String? = null,
    action: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier.padding(start = 58.dp, top = 64.dp).width(650.dp)
            .heightIn(min = if (compact) 176.dp else if (description.isBlank()) 224.dp else 286.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.Top,
    ) {
        avatarUrl?.takeIf(String::isNotBlank)?.let {
            AsyncImage(
                model = it,
                contentDescription = title,
                modifier = Modifier.width(92.dp).height(92.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
        Column(verticalArrangement = Arrangement.Top) {
            Text(eyebrow, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(
                title,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(metadata, color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.titleMedium)
            description.takeIf(String::isNotBlank)?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = Color.White.copy(alpha = .88f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

private fun formatCollectionCount(value: Long): String = when {
    value >= 1_000_000L -> "%.1fM".format(value / 1_000_000.0)
    value >= 1_000L -> "%.1fK".format(value / 1_000.0)
    else -> value.toString()
}

private fun formatSubscriberCount(value: Long): String =
    "${formatCollectionCount(value)} ${if (value == 1L) "subscriber" else "subscribers"}"

internal fun collectionMetadata(count: Long, singular: String, plural: String, owner: String): String =
    listOfNotNull(
        count.takeIf { it >= 0L }?.let { "$it ${if (it == 1L) singular else plural}" },
        owner.takeIf(String::isNotBlank),
    ).joinToString(" · ")

@Composable
internal fun AutoLoadMore(state: LazyListState, hasMore: Boolean, loading: Boolean, onLoadMore: () -> Unit) {
    LaunchedEffect(state, hasMore, loading) {
        snapshotFlow {
            val info = state.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            hasMore && info.totalItemsCount > 0 && last >= info.totalItemsCount - 2
        }.distinctUntilChanged().collectLatest { if (it && !loading) onLoadMore() }
    }
}

@Composable
internal fun CollectionError(message: String) {
    Text(message, modifier = Modifier.padding(horizontal = 58.dp), color = MaterialTheme.colorScheme.error)
}

@Composable
internal fun CollectionEmptyState(title: String, message: String) {
    Box(
        modifier = Modifier.padding(horizontal = 58.dp).width(620.dp)
            .background(Color.White.copy(alpha = .06f), RoundedCornerShape(16.dp))
            .padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = .7f),
            )
        }
    }
}
