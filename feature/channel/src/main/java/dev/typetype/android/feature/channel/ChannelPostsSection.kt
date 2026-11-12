package dev.typetype.android.feature.channel

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.ExternalLinkDialog
import dev.typetype.android.core.ui.components.LazyPaginationFooter

internal data class ChannelPostsActions(
    val onRetry: () -> Unit = {},
    val onLoadMore: () -> Unit = {},
    val onUrlClick: (String) -> Unit = {},
)

@Composable
internal fun rememberChannelPosts(active: Boolean): Pair<ChannelPostsState, ChannelPostsActions> {
    val viewModel: ChannelPostsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingUrl by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    LaunchedEffect(active) { if (active) viewModel.ensureLoaded() }
    pendingUrl?.let { url ->
        ExternalLinkDialog(
            url = url,
            onConfirm = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
                pendingUrl = null
            },
            onDismiss = { pendingUrl = null },
        )
    }
    val actions = remember(viewModel) {
        ChannelPostsActions(
            onRetry = viewModel::ensureLoaded,
            onLoadMore = viewModel::loadMore,
            onUrlClick = { pendingUrl = it },
        )
    }
    return state to actions
}

internal fun LazyGridScope.channelPostsItems(
    state: ChannelPostsState,
    actions: ChannelPostsActions,
    onPlayVideo: (String) -> Unit,
) {
    when {
        state.isLoading && state.posts.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        state.failed && state.posts.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.channel_posts_load_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = actions.onRetry) { Text(stringResource(R.string.state_retry)) }
            }
        }
        state.loaded && state.posts.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.channel_no_posts),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    items(
        items = state.posts,
        key = { "channel-post-${it.id}" },
        span = { GridItemSpan(maxLineSpan) },
        contentType = { "channel-post" },
    ) { post ->
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            CommunityPostCard(
                post = post,
                onPlayVideo = onPlayVideo,
                onUrlClick = actions.onUrlClick,
                modifier = Modifier.widthIn(max = 680.dp).padding(horizontal = 8.dp, vertical = 8.dp),
            )
        }
    }
    item(span = { GridItemSpan(maxLineSpan) }, key = "channel-posts-pagination") {
        LazyPaginationFooter(
            continuationKey = state.nextPage,
            isLoading = state.isLoadingMore,
            hasError = state.loadMoreFailed,
            onLoadMore = actions.onLoadMore,
        )
    }
}
