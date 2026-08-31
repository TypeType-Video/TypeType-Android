package video.typetype.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import video.typetype.sdk.core.Podcast
import video.typetype.sdk.core.PodcastEpisodesPage
import video.typetype.sdk.core.Video

@Composable
internal fun PodcastRow(
    podcasts: List<Podcast>,
    onOpen: (Podcast) -> Unit,
    restoreFocusKey: String? = null,
    onFocused: (String) -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        androidx.compose.foundation.layout.Box(Modifier.padding(horizontal = 58.dp)) { SectionTitle("Podcasts") }
        LazyRow(
            modifier = Modifier.focusRestorer(),
            contentPadding = PaddingValues(horizontal = 58.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(podcasts, key = { it.id }) { podcast ->
                val key = "podcast:${podcast.id}"
                val focusRequester = remember(podcast.id) { FocusRequester() }
                LaunchedEffect(restoreFocusKey) {
                    if (restoreFocusKey == key) focusRequester.requestFocus()
                }
                Column(modifier = Modifier.width(184.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(104.dp).focusRequester(focusRequester)
                            .onFocusChanged { if (it.isFocused) onFocused(key) },
                        onClick = { onOpen(podcast) },
                        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                    ) {
                        AsyncImage(
                            model = podcast.thumbnailUrl,
                            contentDescription = podcast.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Text(podcast.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    podcast.streamCount.takeIf { it >= 0L }?.let { count ->
                        Text(
                            "$count ${if (count == 1L) "episode" else "episodes"}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
public fun PodcastEpisodesScreen(
    page: PodcastEpisodesPage,
    isLoadingMore: Boolean,
    errorMessage: String?,
    onOpenVideo: (Video) -> Unit,
    onLoadMore: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var focusedItem by rememberSaveable(page.podcast.url) {
        mutableStateOf(page.episodes.firstOrNull()?.let { videoFocusKey("Episodes", it) })
    }
    val listState = rememberLazyListState()
    AutoLoadMore(listState, page.nextPage != null, isLoadingMore, onLoadMore)
    CollectionBackdrop(page.episodes.firstOrNull()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().restoreFocusWhen(true),
            state = listState,
            contentPadding = PaddingValues(bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item {
                CollectionHero(
                    eyebrow = "PODCAST",
                    title = page.podcast.title,
                    metadata = collectionMetadata(
                        count = page.podcast.streamCount,
                        singular = "episode",
                        plural = "episodes",
                        owner = page.podcast.uploaderName,
                    ),
                    compact = true,
                )
            }
            if (page.episodes.isEmpty()) {
                item { EmptyScreen("No episodes", "The server did not return any episode for this podcast.") }
            } else item {
                VideoRow(
                    "Episodes", page.episodes, onOpenVideo,
                    restoreFocusKey = focusedItem,
                    focusActive = true,
                    onFocused = { focusedItem = it },
                )
            }
            errorMessage?.let {
                item { Text(it, modifier = Modifier.padding(horizontal = 58.dp), color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
