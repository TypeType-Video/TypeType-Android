package dev.typetype.android.feature.player.livechat

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import dev.typetype.android.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TwitchVodChatSheet(videoUrl: String, player: Player?, onDismiss: () -> Unit) {
    val viewModel: TwitchVodChatViewModel = hiltViewModel(key = "vod-chat")
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel, videoUrl, player) {
        viewModel.start(videoUrl)
        while (true) {
            player?.let { viewModel.onPosition(it.currentPosition) }
            delay(POSITION_TICK_MS)
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = stringResource(R.string.vod_chat_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                val status = when {
                    state.unavailable -> R.string.vod_chat_unavailable
                    !state.ready -> R.string.live_chat_connecting
                    else -> null
                }
                status?.let {
                    Text(
                        text = stringResource(it),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            LiveChatMessages(messages = state.messages, modifier = Modifier.weight(1f))
        }
    }
}

private const val POSITION_TICK_MS = 1_000L
