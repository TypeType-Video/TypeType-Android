package dev.typetype.android.feature.settings.rss

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import dev.typetype.android.core.ui.components.TypeTypeSwitch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.RequestIdRow
import dev.typetype.android.domain.rss.RssFeed
import dev.typetype.android.domain.rss.RssFeedScope
import dev.typetype.android.feature.settings.SettingsDetailTopBar

@Composable
fun RssFeedsRoute(
    onNavigateBack: () -> Unit,
    viewModel: RssFeedsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RssFeedsScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onAction = viewModel::onAction,
    )
}

@Composable
internal fun RssFeedsScreen(
    state: RssFeedsState,
    onNavigateBack: () -> Unit,
    onAction: (RssFeedsAction) -> Unit,
) {
    val channelNames = state.subscriptions.associate { it.channelUrl to it.name }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsDetailTopBar(
                title = stringResource(R.string.rss_settings_title),
                onNavigateBack = onNavigateBack,
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { RssLimitsCard(state) }
                state.errorMessage?.let { message ->
                    item {
                        RssFailureCard(
                            message = message,
                            requestId = state.errorRequestId,
                            onRetry = { onAction(RssFeedsAction.Retry) },
                            onDismiss = { onAction(RssFeedsAction.DismissFailure) },
                        )
                    }
                }
                if (!state.isLoading && state.feeds.isEmpty()) {
                    item { RssEmptyCard() }
                }
                items(state.feeds, key = RssFeed::id) { feed ->
                    RssFeedCard(
                        feed = feed,
                        channelNames = channelNames,
                        isMutating = state.isMutating,
                        onAction = onAction,
                    )
                }
                item {
                    Button(
                        onClick = { onAction(RssFeedsAction.Create) },
                        enabled = state.canCreate && !state.isMutating,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(
                            text = stringResource(R.string.rss_create),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
    state.editor?.let {
        RssFeedEditorDialog(
            state = it,
            subscriptions = state.subscriptions,
            availableServiceIds = state.availableServiceIds,
            isSaving = state.isMutating,
            onAction = onAction,
        )
    }
    state.secret?.let {
        RssFeedSecretDialog(
            feedName = it.feedName,
            url = it.url,
            onDismiss = { onAction(RssFeedsAction.DismissSecret) },
        )
    }
    if (state.regeneratingFeedId != null) {
        RssRegenerateDialog(
            enabled = !state.isMutating,
            onConfirm = { onAction(RssFeedsAction.ConfirmRegenerate) },
            onDismiss = { onAction(RssFeedsAction.DismissRegenerate) },
        )
    }
    if (state.deletingFeedId != null) {
        RssDeleteDialog(
            enabled = !state.isMutating,
            onConfirm = { onAction(RssFeedsAction.ConfirmDelete) },
            onDismiss = { onAction(RssFeedsAction.DismissDelete) },
        )
    }
}

@Composable
private fun RssLimitsCard(state: RssFeedsState) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(R.string.rss_private_notice), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(
                    R.string.rss_limits,
                    state.feeds.size,
                    state.capability.maxFeedsPerUser,
                    state.capability.maxItems,
                    state.capability.minimumPollMinutes,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RssFeedCard(
    feed: RssFeed,
    channelNames: Map<String, String>,
    isMutating: Boolean,
    onAction: (RssFeedsAction) -> Unit,
) {
    val enabledDescription = stringResource(
        R.string.rss_enabled_description,
        feed.name,
        stringResource(if (feed.enabled) R.string.rss_enabled else R.string.rss_disabled),
    )
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        feed.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (feed.scope == RssFeedScope.All) {
                            stringResource(R.string.rss_scope_all)
                        } else {
                            feed.channelUrls.joinToString(", ") { channelNames[it] ?: it }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    RssFeedSummary(feed)
                }
                TypeTypeSwitch(
                    checked = feed.enabled,
                    enabled = !isMutating,
                    onCheckedChange = { onAction(RssFeedsAction.SetEnabled(feed.id, it)) },
                    modifier = Modifier.semantics {
                        contentDescription = enabledDescription
                    },
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(
                    enabled = !isMutating,
                    onClick = { onAction(RssFeedsAction.Edit(feed.id)) },
                ) {
                    Icon(Icons.Filled.Edit, stringResource(R.string.rss_edit))
                }
                IconButton(
                    enabled = !isMutating,
                    onClick = { onAction(RssFeedsAction.RequestRegenerate(feed.id)) },
                ) {
                    Icon(Icons.Filled.Refresh, stringResource(R.string.rss_regenerate))
                }
                IconButton(
                    enabled = !isMutating,
                    onClick = { onAction(RssFeedsAction.RequestDelete(feed.id)) },
                ) {
                    Icon(Icons.Filled.Delete, stringResource(R.string.rss_delete))
                }
            }
        }
    }
}

@Composable
private fun RssEmptyCard() {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Text(
            text = stringResource(R.string.rss_empty),
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RssFailureCard(
    message: String,
    requestId: String?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.large)
            .padding(12.dp),
    ) {
        Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
        requestId?.let { RequestIdRow(it) }
        Row(modifier = Modifier.align(Alignment.End)) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_dismiss)) }
            TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
        }
    }
}
