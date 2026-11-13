package dev.typetype.android.feature.player.livechat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.typetype.android.R
import dev.typetype.android.core.ui.share.LocalServerBaseUrl
import dev.typetype.android.core.url.buildImageUrl
import dev.typetype.android.domain.livechat.LiveChatMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LiveChatSheet(videoUrl: String, onDismiss: () -> Unit) {
    val viewModel: LiveChatViewModel = hiltViewModel(key = "live-chat")
    val state by viewModel.state.collectAsStateWithLifecycle()
    DisposableEffect(viewModel, videoUrl) {
        viewModel.start(videoUrl)
        onDispose { viewModel.stop() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f)) {
            LiveChatHeader(status = state.status, onRetry = { viewModel.retry(videoUrl) })
            LiveChatMessages(messages = state.messages, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LiveChatHeader(status: LiveChatStatus, onRetry: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.live_chat_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = stringResource(status.labelResource()),
                style = MaterialTheme.typography.labelMedium,
                color = if (status == LiveChatStatus.Failed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        if (status == LiveChatStatus.Failed) {
            TextButton(onClick = onRetry) { Text(stringResource(R.string.state_retry)) }
        }
    }
}

@Composable
private fun LiveChatMessages(messages: List<LiveChatMessage>, modifier: Modifier) {
    val listState = rememberLazyListState()
    val following by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= listState.layoutInfo.totalItemsCount - 2
        }
    }
    LaunchedEffect(messages.lastOrNull()?.id) {
        if (following && messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex)
    }
    if (messages.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.live_chat_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(messages, key = { it.id }) { LiveChatMessageRow(it) }
    }
}

@Composable
private fun LiveChatMessageRow(message: LiveChatMessage) {
    Row(verticalAlignment = Alignment.Top) {
        AsyncImage(
            model = message.authorAvatarUrl?.let { buildImageUrl(LocalServerBaseUrl.current, it) },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        val authorColor = if (message.moderator) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
        Text(
            text = buildAnnotatedString {
                message.authorName?.let {
                    withStyle(SpanStyle(color = authorColor, fontWeight = FontWeight.Medium)) { append(it) }
                    append("  ")
                }
                append(message.text)
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

private fun LiveChatStatus.labelResource(): Int = when (this) {
    LiveChatStatus.Connecting -> R.string.live_chat_connecting
    LiveChatStatus.Live -> R.string.live_chat_live
    LiveChatStatus.Reconnecting -> R.string.live_chat_reconnecting
    LiveChatStatus.Ended -> R.string.live_chat_ended
    LiveChatStatus.Failed -> R.string.live_chat_failed
}
