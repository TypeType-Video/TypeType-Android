package dev.typetype.android.feature.player.components

import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import dev.typetype.android.domain.comments.BulletCommentPosition
import dev.typetype.android.feature.player.PlayerDanmakuState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
internal fun DanmakuOverlay(
    player: Player,
    state: PlayerDanmakuState,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val accessibilityManager = remember(context) {
        context.getSystemService(AccessibilityManager::class.java)
    }
    var touchExplorationEnabled by remember(accessibilityManager) {
        mutableStateOf(accessibilityManager?.isTouchExplorationEnabled == true)
    }
    DisposableEffect(accessibilityManager) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener {
            touchExplorationEnabled = it
        }
        accessibilityManager?.addTouchExplorationStateChangeListener(listener)
        onDispose {
            accessibilityManager?.removeTouchExplorationStateChangeListener(listener)
        }
    }
    if (!visible || !state.available || !state.enabled || touchExplorationEnabled) return

    var positionMillis by remember(player) { mutableLongStateOf(player.currentPosition.coerceAtLeast(0L)) }
    LaunchedEffect(player) {
        while (true) {
            positionMillis = player.currentPosition.coerceAtLeast(0L)
            delay(if (player.isPlaying) 33L else 200L)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .clearAndSetSemantics {},
    ) {
        val density = LocalDensity.current
        val laneHeightPx = with(density) { LANE_HEIGHT.toPx() }
        val laneCount = minOf(
            DANMAKU_LANES,
            ((constraints.maxHeight * OVERLAY_HEIGHT_FRACTION) / laneHeightPx)
                .toInt()
                .coerceAtLeast(1),
        )
        val presented = remember(state.comments, positionMillis, state.speed, laneCount) {
            presentBulletComments(
                comments = state.comments,
                positionMillis = positionMillis,
                speed = state.speed,
                laneCount = laneCount,
            )
        }
        val widthPx = constraints.maxWidth.toFloat()
        presented.forEach { item ->
            val comment = item.comment
            val fontScale = comment.relativeFontSize * state.size
            val estimatedWidth = with(density) {
                (comment.text.length * CHARACTER_WIDTH * fontScale).dp.toPx()
            }
            val x = if (comment.position == BulletCommentPosition.Regular) {
                widthPx - item.progress * (widthPx + estimatedWidth)
            } else {
                (widthPx - estimatedWidth) / 2f
            }
            Box(
                modifier = Modifier.offset {
                    IntOffset(
                        x = x.roundToInt(),
                        y = (item.lane * laneHeightPx).roundToInt(),
                    )
                },
            ) {
                Text(
                    text = comment.text,
                    color = Color(0xFF000000L or (comment.rgbColor.toLong() and 0x00FFFFFFL)),
                    fontSize = (BASE_FONT_SIZE * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black,
                            offset = Offset(1f, 1f),
                            blurRadius = 2f,
                        ),
                    ),
                )
            }
        }
    }
}

private val LANE_HEIGHT = 36.dp
private const val BASE_FONT_SIZE = 20f
private const val CHARACTER_WIDTH = 14f
private const val OVERLAY_HEIGHT_FRACTION = 0.6f
