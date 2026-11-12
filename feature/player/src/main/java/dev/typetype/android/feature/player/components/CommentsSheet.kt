package dev.typetype.android.feature.player.components

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import dev.typetype.android.R
import dev.typetype.android.domain.comments.Comment
import dev.typetype.android.domain.comments.CommentsRepository
import dev.typetype.android.domain.navigation.sameVideoTimestampMillis
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(
    pagingFlow: Flow<PagingData<Comment>>,
    videoUrl: String,
    commentsRepository: CommentsRepository,
    onDismiss: () -> Unit,
    onTimestampClick: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val items: LazyPagingItems<Comment> = pagingFlow.collectAsLazyPagingItems()
    val context = LocalContext.current
    var pendingUrl by remember { mutableStateOf<String?>(null) }
    val replies = rememberCommentRepliesController(videoUrl, commentsRepository::loadReplies)
    val voiceReplyPlayback = rememberVoiceReplyPlayback(commentsRepository::loadVoiceReplyAudio)
    val onUrlClick: (String) -> Unit = { url ->
        val timestamp = sameVideoTimestampMillis(url, videoUrl)
        if (timestamp != null) onTimestampClick(timestamp) else pendingUrl = url
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        CompositionLocalProvider(LocalVoiceReplyPlayback provides voiceReplyPlayback) {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f)) {
                Text(
                    text = stringResource(R.string.comments_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                CommentsList(
                    items = items,
                    replies = replies,
                    onUrlClick = onUrlClick,
                    onTimestampClick = onTimestampClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    pendingUrl?.let { url ->
        ExternalLinkDialog(
            url = url,
            onConfirm = {
                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                pendingUrl = null
            },
            onDismiss = { pendingUrl = null },
        )
    }
}
