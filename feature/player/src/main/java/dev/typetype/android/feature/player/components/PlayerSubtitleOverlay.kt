package dev.typetype.android.feature.player.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.ui.SubtitleView
import dev.typetype.android.R
import dev.typetype.android.domain.stream.StreamSubtitleSource
import dev.typetype.android.domain.usersettings.CaptionStyles
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
    captionStyles: CaptionStyles = CaptionStyles(),
) {
    var nativeCues by remember(player) { mutableStateOf(emptyList<Cue>()) }
    var externalCues by remember(externalSource?.url) {
        mutableStateOf(emptyList<CuesWithTiming>())
    }
    var activeExternalCues by remember(externalSource?.url) {
        mutableStateOf(emptyList<Cue>())
    }
    var externalLoadFailed by remember(externalSource?.url) { mutableStateOf(false) }
    val temporarilyUnavailable = stringResource(
        R.string.player_subtitles_temporarily_unavailable,
    )

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
        val result = externalSource?.let { loadExternalCues(it) }
        externalCues = result?.getOrDefault(emptyList()).orEmpty()
        externalLoadFailed = result?.isFailure == true
        if (externalLoadFailed) {
            delay(EXTERNAL_FAILURE_VISIBILITY_MS)
            externalLoadFailed = false
        }
    }

    LaunchedEffect(player, externalSource?.url, externalCues) {
        if (externalSource == null) {
            activeExternalCues = emptyList()
            return@LaunchedEffect
        }
        while (isActive) {
            val positionUs = player.currentPosition.coerceAtLeast(0L) * 1_000L
            activeExternalCues = externalCues.activeAt(positionUs)
            delay(CUE_REFRESH_INTERVAL_MS)
        }
    }

    if (!subtitlesVisible) return
    val cues = if (externalSource == null) nativeCues else activeExternalCues
    val text = cues.subtitleText().ifBlank {
        temporarilyUnavailable.takeIf { externalLoadFailed }.orEmpty()
    }
    if (text.isBlank()) return
    val displayedCues = cues.ifEmpty {
        listOf(Cue.Builder().setText(text).build())
    }

    AndroidView(
        factory = { context ->
            SubtitleView(context).apply {
                setBottomPaddingFraction(0f)
                applyCaptionStyle(captionStyles)
                setCues(displayedCues)
            }
        },
        update = { subtitleView ->
            subtitleView.applyCaptionStyle(captionStyles)
            subtitleView.setCues(displayedCues)
        },
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
            .padding(horizontal = 24.dp)
            .padding(bottom = if (controlsVisible) 132.dp else 34.dp)
            .semantics { this.text = AnnotatedString(text) },
    )
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

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
internal fun List<CuesWithTiming>.activeAt(positionUs: Long): List<Cue> =
    filter { positionUs >= it.startTimeUs && positionUs < it.endTimeUs }
        .flatMap(CuesWithTiming::cues)

private const val CUE_REFRESH_INTERVAL_MS = 100L
private const val EXTERNAL_FAILURE_VISIBILITY_MS = 4_000L
