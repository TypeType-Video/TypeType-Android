package dev.typetype.android.feature.player.components

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import dev.typetype.android.R

@OptIn(markerClass = [UnstableApi::class])
@Composable
fun MiniPlayerBar(
    player: Player,
    title: String,
    subtitle: String,
    onExpand: () -> Unit,
    onSendToBackground: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    sleepTimerLabel: String? = null,
) {
    val playPauseState = rememberPlayPauseButtonState(player)
    val backgroundActionLabel = stringResource(R.string.mini_player_send_to_background)
    val expandActionLabel = stringResource(R.string.mini_player_expand)
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { MINI_PLAYER_SWIPE_THRESHOLD.toPx() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .pointerInput(onExpand, onSendToBackground) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial,
                    )
                    val startY = down.position.y
                    var handled = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed || handled) break
                        val deltaY = change.position.y - startY
                        when {
                            deltaY >= swipeThresholdPx -> {
                                change.consume()
                                handled = true
                                onSendToBackground()
                            }
                        }
                    }
                }
            }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(backgroundActionLabel) {
                        onSendToBackground()
                        true
                    },
                )
            }
            .clickable(onClickLabel = expandActionLabel, onClick = onExpand),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .width(80.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Transparent),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (sleepTimerLabel != null) {
            Icon(
                imageVector = Icons.Filled.Bedtime,
                contentDescription = sleepTimerLabel,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
        IconButton(onClick = { if (playPauseState.isEnabled) playPauseState.onClick() }) {
            Icon(
                painter = painterResource(
                    if (playPauseState.showPlay) R.drawable.ic_play else R.drawable.ic_pause,
                ),
                contentDescription = stringResource(R.string.player_play_pause),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.mini_player_close),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private val MINI_PLAYER_SWIPE_THRESHOLD = 36.dp
