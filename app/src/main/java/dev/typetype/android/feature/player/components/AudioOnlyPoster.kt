package dev.typetype.android.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.typetype.android.R

@Composable
internal fun AudioOnlyPoster(
    thumbnailUrl: String,
    title: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.24f),
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f)))
        BoxWithConstraints(contentAlignment = Alignment.Center) {
            if (maxHeight < 280.dp) {
                AudioOnlyCompactContent(thumbnailUrl = thumbnailUrl, title = title)
            } else {
                AudioOnlyExpandedContent(thumbnailUrl = thumbnailUrl, title = title)
            }
        }
    }
}

@Composable
private fun AudioOnlyExpandedContent(thumbnailUrl: String, title: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 32.dp),
    ) {
        AudioOnlyArtwork(thumbnailUrl = thumbnailUrl, size = 112.dp)
        AudioOnlyDetails(title = title, centered = true)
    }
}

@Composable
private fun AudioOnlyCompactContent(thumbnailUrl: String, title: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp),
    ) {
        AudioOnlyArtwork(thumbnailUrl = thumbnailUrl, size = 88.dp)
        Column(modifier = Modifier.weight(1f)) {
            AudioOnlyDetails(title = title, centered = false)
        }
    }
}

@Composable
private fun AudioOnlyArtwork(thumbnailUrl: String, size: androidx.compose.ui.unit.Dp) {
    AsyncImage(
        model = thumbnailUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(size).clip(RoundedCornerShape(18.dp)),
    )
}

@Composable
private fun AudioOnlyDetails(title: String, centered: Boolean) {
    Icon(
        imageVector = Icons.Filled.Headphones,
        contentDescription = stringResource(R.string.player_audio_only),
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(26.dp),
    )
    Text(
        text = title,
        color = Color.White,
        style = MaterialTheme.typography.titleMedium,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(vertical = 8.dp),
    )
    AudioOnlyBars()
}

@Composable
private fun AudioOnlyBars() {
    val transition = rememberInfiniteTransition(label = "audioWave")
    val heights = listOf(520, 690, 430, 760, 590).mapIndexed { index, duration ->
        val height by transition.animateFloat(
            initialValue = 0.28f + index * 0.04f,
            targetValue = 1f - index * 0.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = duration),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "audioWave$index",
        )
        height
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.size(width = 54.dp, height = 22.dp),
    ) {
        heights.forEach { height ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(height)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
