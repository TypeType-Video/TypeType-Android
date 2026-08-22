package dev.typetype.android.feature.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.AnimatedError
import dev.typetype.android.core.ui.components.FullScreenLoader
import dev.typetype.android.core.ui.components.LazyPaginationFooter
import dev.typetype.android.core.ui.components.RequestIdRow

@Composable
fun NotificationsRoute(
    onNavigateBack: () -> Unit,
    onPlayVideo: (String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NotificationsScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onPlayVideo = onPlayVideo,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    state: NotificationsState,
    onNavigateBack: () -> Unit,
    onPlayVideo: (String) -> Unit,
    onAction: (NotificationsAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(stringResource(R.string.notifications_title))
                    if (state.unreadCount > 0) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.notifications_unread_count,
                                state.unreadCount,
                                state.unreadCount,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.settings_back),
                    )
                }
            },
            actions = {
                if (state.unreadCount > 0) {
                    IconButton(
                        onClick = { onAction(NotificationsAction.MarkAllRead) },
                        enabled = !state.isMarkingRead,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DoneAll,
                            contentDescription = stringResource(R.string.notifications_mark_all_read),
                        )
                    }
                }
            },
            windowInsets = WindowInsets(0, 0, 0, 0),
        )
        if (state.isMarkingRead) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        PullToRefreshBox(
            isRefreshing = state.isLoading && state.items.isNotEmpty(),
            onRefresh = { onAction(NotificationsAction.Retry) },
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading && state.items.isEmpty() -> FullScreenLoader()
                    state.errorMessage != null && state.items.isEmpty() -> AnimatedError(
                        message = state.errorMessage,
                        requestId = state.errorRequestId,
                        onRetry = { onAction(NotificationsAction.Retry) },
                    )
                    state.items.isEmpty() -> EmptyNotifications()
                    else -> NotificationList(state, onPlayVideo, onAction)
                }
            }
        }
    }
}

@Composable
private fun NotificationList(
    state: NotificationsState,
    onPlayVideo: (String) -> Unit,
    onAction: (NotificationsAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    ) {
        state.actionErrorMessage?.let { message ->
            item(key = "action-error") {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(message, color = MaterialTheme.colorScheme.error)
                    state.actionErrorRequestId?.let { RequestIdRow(requestId = it) }
                }
            }
        }
        itemsIndexed(
            items = state.items,
            key = { _, item -> "${item.type}:${item.video.id}:${item.createdAtMillis}" },
            contentType = { _, _ -> "notification" },
        ) { index, item ->
            NotificationRow(
                item = item,
                onOpenVideo = { onPlayVideo(item.video.url) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (index < state.items.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        item(key = "notifications-pagination") {
            LazyPaginationFooter(
                continuationKey = state.nextPage,
                isLoading = state.isLoadingMore,
                hasError = state.loadMoreError,
                onLoadMore = { onAction(NotificationsAction.LoadMore) },
            )
        }
    }
}

@Composable
private fun EmptyNotifications() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.NotificationsNone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.notifications_empty),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
