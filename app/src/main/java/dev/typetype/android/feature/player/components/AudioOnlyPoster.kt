package dev.typetype.android.feature.player.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.BitmapImage
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import dev.typetype.android.core.ui.share.LocalServerBaseUrl
import dev.typetype.android.core.ui.share.buildImageUrl

@Composable
internal fun AudioOnlyPoster(
    thumbnailUrl: String,
    title: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    viewModel: AudioOnlyPosterViewModel = hiltViewModel(),
) {
    var palette by remember(thumbnailUrl) { mutableStateOf(AudioOnlyPalette.Default) }
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(
                    palette.primary.copy(alpha = 0.72f),
                    palette.secondary.copy(alpha = 0.38f),
                    Color.Black,
                ),
            ),
        ),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(contentAlignment = Alignment.Center) {
            if (maxHeight < 280.dp) {
                AudioOnlyCompactContent(
                    thumbnailUrl,
                    title,
                    isPlaying,
                    viewModel,
                    palette,
                    onPalette = { palette = it },
                )
            } else {
                AudioOnlyExpandedContent(
                    thumbnailUrl,
                    title,
                    isPlaying,
                    viewModel,
                    palette,
                    onPalette = { palette = it },
                )
            }
        }
    }
}

@Composable
private fun AudioOnlyExpandedContent(
    thumbnailUrl: String,
    title: String,
    isPlaying: Boolean,
    viewModel: AudioOnlyPosterViewModel,
    palette: AudioOnlyPalette,
    onPalette: (AudioOnlyPalette) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
    ) {
        AudioOnlyArtwork(thumbnailUrl, 112.dp, onPalette)
        AudioOnlyTitle(title, centered = true)
        AudioOnlyWaveform(viewModel, isPlaying, palette, Modifier.fillMaxWidth().height(64.dp))
    }
}

@Composable
private fun AudioOnlyCompactContent(
    thumbnailUrl: String,
    title: String,
    isPlaying: Boolean,
    viewModel: AudioOnlyPosterViewModel,
    palette: AudioOnlyPalette,
    onPalette: (AudioOnlyPalette) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 18.dp),
    ) {
        AudioOnlyArtwork(thumbnailUrl, 88.dp, onPalette)
        Column(modifier = Modifier.weight(1f)) {
            AudioOnlyTitle(title, centered = false)
            AudioOnlyWaveform(viewModel, isPlaying, palette, Modifier.fillMaxWidth().height(42.dp))
        }
    }
}

@Composable
private fun AudioOnlyArtwork(
    thumbnailUrl: String,
    size: Dp,
    onPalette: (AudioOnlyPalette) -> Unit,
) {
    val context = LocalContext.current
    val serverBaseUrl = LocalServerBaseUrl.current
    val request = remember(context, serverBaseUrl, thumbnailUrl) {
        ImageRequest.Builder(context)
            .data(buildImageUrl(serverBaseUrl, thumbnailUrl))
            .size(224)
            .allowHardware(false)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        onSuccess = { success ->
            (success.result.image as? BitmapImage)?.bitmap?.let {
                onPalette(it.audioOnlyPalette())
            }
        },
        modifier = Modifier.size(size).clip(RoundedCornerShape(18.dp)),
    )
}

@Composable
private fun AudioOnlyTitle(title: String, centered: Boolean) {
    androidx.compose.material3.Text(
        text = title,
        color = Color.White,
        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun AudioOnlyWaveform(
    viewModel: AudioOnlyPosterViewModel,
    isPlaying: Boolean,
    palette: AudioOnlyPalette,
    modifier: Modifier,
) {
    val levels by viewModel.levels.collectAsStateWithLifecycle()
    Canvas(modifier) {
        val count = levels.size.coerceAtLeast(1)
        val gap = 4.dp.toPx()
        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(2.dp.toPx())
        val brush = Brush.verticalGradient(listOf(palette.highlight, Color.White))
        levels.forEachIndexed { index, level ->
            val activeLevel = if (isPlaying) {
                level.coerceAtLeast(ACTIVE_WAVEFORM_FLOOR)
            } else {
                0f
            }
            val barHeight = (size.height * activeLevel).coerceAtLeast(3.dp.toPx())
            val x = index * (barWidth + gap)
            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, (size.height - barHeight) / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

private data class AudioOnlyPalette(
    val primary: Color,
    val secondary: Color,
    val highlight: Color,
) {
    companion object {
        val Default = AudioOnlyPalette(Color(0xFF1D4ED8), Color(0xFF111827), Color(0xFF60A5FA))
    }
}

private fun Bitmap.audioOnlyPalette(): AudioOnlyPalette {
    var red = 0L
    var green = 0L
    var blue = 0L
    var count = 0
    repeat(SAMPLE_GRID) { row ->
        repeat(SAMPLE_GRID) { column ->
            val pixel = getPixel(
                ((column + 0.5f) * width / SAMPLE_GRID).toInt().coerceIn(0, width - 1),
                ((row + 0.5f) * height / SAMPLE_GRID).toInt().coerceIn(0, height - 1),
            )
            red += android.graphics.Color.red(pixel)
            green += android.graphics.Color.green(pixel)
            blue += android.graphics.Color.blue(pixel)
            count += 1
        }
    }
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (red / count).toInt(),
        (green / count).toInt(),
        (blue / count).toInt(),
        hsv,
    )
    hsv[1] = hsv[1].coerceAtLeast(0.58f)
    hsv[2] = hsv[2].coerceIn(0.42f, 0.72f)
    val primary = Color(android.graphics.Color.HSVToColor(hsv))
    val shifted = hsv.copyOf().also { it[0] = (it[0] + 32f) % 360f }
    val highlighted = hsv.copyOf().also { it[2] = 1f }
    return AudioOnlyPalette(
        primary = primary,
        secondary = Color(android.graphics.Color.HSVToColor(shifted)),
        highlight = Color(android.graphics.Color.HSVToColor(highlighted)),
    )
}

private const val SAMPLE_GRID = 6
private const val ACTIVE_WAVEFORM_FLOOR = 0.34f
