package dev.typetype.android.feature.player.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private const val BAR_COUNT = 112
private const val WAVE_SEED = 0x525459

private class WaveMotion(count: Int, seed: Int) {
    private val random = Random(seed)
    val levels = FloatArray(count) { random.nextFloat() }
    val targets = FloatArray(count) { random.nextFloat() }
    val phases = FloatArray(count) { random.nextFloat() * 2f * PI.toFloat() }
    val speeds = FloatArray(count) { 0.018f + random.nextFloat() * 0.055f }
    val intervals = IntArray(count) { 4 + random.nextInt(10) }
    val offsets = IntArray(count) { random.nextInt(intervals[it]) }

    fun nextTarget(index: Int): Float {
        val target = random.nextFloat()
        targets[index] = target
        return target
    }
}

@Composable
internal fun AudioOnlyWaveform(
    isPlaying: Boolean,
    palette: AudioOnlyPalette,
    modifier: Modifier,
) {
    var frame by remember { mutableIntStateOf(0) }
    val motion = remember { WaveMotion(BAR_COUNT, WAVE_SEED) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { }
            frame += 1
        }
    }

    Canvas(modifier) {
        val slotWidth = size.width / motion.levels.size
        val barWidth = slotWidth * 0.71f
        val gap = slotWidth - barWidth
        val signal = if (isPlaying) 0.34f else 0f
        val brush = Brush.verticalGradient(
            colors = listOf(
                palette.highlight.copy(alpha = 0.25f),
                palette.primary.copy(alpha = 0.66f),
                palette.secondary.copy(alpha = 0.16f),
            ),
        )

        repeat(motion.levels.size) { index ->
            val interval = motion.intervals[index].coerceAtLeast(1)
            if ((frame + motion.offsets[index]) % interval == 0) {
                motion.nextTarget(index)
            }
            motion.levels[index] += (
                motion.targets[index] - motion.levels[index]
                ) * 0.11f
            val phase = frame * motion.speeds[index] + motion.phases[index]
            val pulse = 0.5f + sin(phase) * 0.5f
            val movement = signal * (
                0.08f + motion.levels[index] * 0.52f + pulse * 0.26f
                )
            val requested = if (isPlaying) 0.035f + movement else 0.025f
            motion.levels[index] += (
                requested.coerceIn(0f, 1f) - motion.levels[index]
                ) * if (isPlaying) 0.22f else 0.08f

            val level = motion.levels[index]
            val edgeFade = 0.65f + sin(
                index.toFloat() / motion.levels.size * PI.toFloat(),
            ) * 0.35f
            val barHeight = maxOf(
                size.height * 0.025f,
                level * edgeFade * size.height * 0.58f,
            )
            drawRoundRect(
                brush = brush,
                topLeft = Offset(index * slotWidth + gap / 2f, (size.height - barHeight) / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}
