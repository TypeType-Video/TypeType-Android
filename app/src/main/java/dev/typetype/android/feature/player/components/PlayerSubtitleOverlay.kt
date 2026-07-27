package dev.typetype.android.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming
import dev.typetype.android.domain.stream.StreamSubtitleSource
import dev.typetype.android.feature.player.LoadSubtitleCues
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
@kotlin.OptIn(ExperimentalLayoutApi::class)
@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
fun PlayerSubtitleOverlay(
    player: Player,
    controlsVisible: Boolean,
    subtitlesVisible: Boolean,
    modifier: Modifier = Modifier,
    externalSource: StreamSubtitleSource? = null,
    loadExternalCues: LoadSubtitleCues = { Result.success(emptyList()) },
) {
    var nativeCues by remember(player) { mutableStateOf(emptyList<Cue>()) }
    var externalCues by remember(externalSource?.url) {
        mutableStateOf(emptyList<CuesWithTiming>())
    }
    var activeExternalCues by remember(externalSource?.url) {
        mutableStateOf(emptyList<Cue>())
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onCues(cueGroup: CueGroup) {
                nativeCues = cueGroup.cues
            }
        }
        nativeCues = player.currentCues.cues
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(externalSource?.url) {
        externalCues = externalSource
            ?.let { loadExternalCues(it).getOrDefault(emptyList()) }
            .orEmpty()
    }

    LaunchedEffect(player, externalSource?.url, externalCues) {
        if (externalSource == null) {
            activeExternalCues = emptyList()
            return@LaunchedEffect
        }
        while (isActive) {
            val positionUs = player.currentPosition.coerceAtLeast(0L) * 1_000L
            activeExternalCues = externalCues
                .filter { positionUs >= it.startTimeUs && positionUs < it.endTimeUs }
                .flatMap(CuesWithTiming::cues)
            delay(CUE_REFRESH_INTERVAL_MS)
        }
    }

    if (!subtitlesVisible) return
    val cues = if (externalSource == null) nativeCues else activeExternalCues
    val text = cues.subtitleText()
    if (text.isBlank()) return

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
                .padding(horizontal = 24.dp)
                .padding(bottom = if (controlsVisible) 132.dp else 34.dp)
                .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

private fun List<Cue>.subtitleText(): String =
    flatMap { cue ->
        cue.text
            ?.toString()
            ?.replace(zeroWidthCharacters, "")
            ?.lineSequence()
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            ?.toList()
            .orEmpty()
    }
        .distinct()
        .joinToString("\n")

private val zeroWidthCharacters = Regex("[\\u200B\\u200C\\u200D\\uFEFF]")

private const val CUE_REFRESH_INTERVAL_MS = 100L
