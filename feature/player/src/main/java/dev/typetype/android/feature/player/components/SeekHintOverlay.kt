package dev.typetype.android.feature.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.typetype.android.feature.player.state.GestureSide
import dev.typetype.android.feature.player.state.PlayerGestureState
import dev.typetype.android.R
import kotlinx.coroutines.delay
import kotlin.math.abs

private const val SEEK_HINT_HOLD_MS = 700L
private const val CHEVRON_CYCLE_MS = 720
private const val CHEVRON_COUNT = 3
private val PanelColor = Color.Black.copy(alpha = 0.34f)

@Composable
internal fun SeekHintOverlay(state: PlayerGestureState) {
    val visible = state.seekHintVisible.value
    val pulse = state.seekHintPulse.longValue
    LaunchedEffect(pulse) {
        if (state.seekHintVisible.value) {
            delay(SEEK_HINT_HOLD_MS)
            state.hideSeekHint()
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(durationMillis = 90)),
        exit = fadeOut(tween(durationMillis = 220)),
        modifier = Modifier.fillMaxSize(),
    ) {
        val side = state.seekHintSide.value
        Box(modifier = Modifier.fillMaxSize()) {
            SeekSidePanel(
                side = side,
                seconds = state.seekHintSeconds.intValue,
                pulse = pulse,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.4f)
                    .align(if (side == GestureSide.Left) Alignment.CenterStart else Alignment.CenterEnd),
            )
        }
    }
}

@Composable
private fun SeekSidePanel(side: GestureSide, seconds: Int, pulse: Long, modifier: Modifier) {
    val bump = remember { Animatable(1f) }
    LaunchedEffect(pulse) {
        bump.snapTo(1.14f)
        bump.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
    }
    val forward = side == GestureSide.Right
    val label = stringResource(
        if (forward) R.string.player_seek_forward_hint else R.string.player_seek_rewind_hint,
        seconds,
    )
    val announcement = pluralStringResource(
        if (forward) R.plurals.player_seek_forward_announcement else R.plurals.player_seek_rewind_announcement,
        seconds,
        seconds,
    )
    Box(
        modifier = modifier
            .clipToBounds()
            .drawBehind {
                val radius = size.height * 0.9f
                val centerX = if (forward) radius else size.width - radius
                drawCircle(PanelColor, radius = radius, center = Offset(centerX, size.height / 2f))
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SeekChevrons(forward = forward)
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = bump.value
                        scaleY = bump.value
                    }
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = announcement
                    },
            )
        }
    }
}

@Composable
private fun SeekChevrons(forward: Boolean) {
    val transition = rememberInfiniteTransition(label = "seekChevrons")
    val cycle: State<Float> = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(CHEVRON_CYCLE_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "seekChevronCycle",
    )
    Canvas(
        modifier = Modifier
            .size(width = 54.dp, height = 18.dp)
            .graphicsLayer { scaleX = if (forward) 1f else -1f },
    ) {
        val step = size.width / CHEVRON_COUNT
        val height = size.height
        val width = height * 0.8f
        repeat(CHEVRON_COUNT) { index ->
            val phase = ((cycle.value - index / CHEVRON_COUNT.toFloat()) % 1f + 1f) % 1f
            val glow = 1f - abs(phase * 2f - 1f)
            val left = index * step + (step - width) / 2f
            val path = Path().apply {
                moveTo(left, 0f)
                lineTo(left + width, height / 2f)
                lineTo(left, height)
                close()
            }
            drawPath(path, Color.White.copy(alpha = 0.25f + 0.75f * glow))
        }
    }
}
