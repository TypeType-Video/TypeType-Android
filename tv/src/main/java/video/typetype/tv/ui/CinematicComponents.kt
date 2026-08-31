package video.typetype.tv.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import video.typetype.sdk.core.Video
import video.typetype.tv.ui.theme.LocalTvAppearance

@Composable
internal fun CinematicBackdrop(video: Video?, modifier: Modifier = Modifier) {
    CinematicBackdrop(video?.thumbnailUrl, modifier)
}

@Composable
internal fun CinematicBackdrop(imageUrl: String?, modifier: Modifier = Modifier) {
    val background = Color(0xFF08090C)
    Box(modifier = modifier.graphicsLayer().background(background)) {
        Crossfade(
            targetState = imageUrl.orEmpty(),
            animationSpec = tween(LocalTvAppearance.current.transitionMillis.coerceAtLeast(1)),
            label = "cinematic-backdrop",
        ) { imageUrl ->
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                )
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to background,
                    .34f to background.copy(alpha = .86f),
                    .72f to background.copy(alpha = .2f),
                    1f to background.copy(alpha = .45f),
                ),
            ),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to background.copy(alpha = .22f),
                    .52f to Color.Transparent,
                    .78f to background.copy(alpha = .88f),
                    1f to background,
                ),
            ),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun VideoRow(
    title: String,
    videos: List<Video>,
    onOpenVideo: (Video) -> Unit,
    progressByVideoId: Map<String, Long> = emptyMap(),
    restoreFocusKey: String? = null,
    focusActive: Boolean = false,
    cinematic: Boolean = true,
    revealFocusedDetails: Boolean = false,
    onFocused: (String) -> Unit = {},
    onPreviewVideo: (Video) -> Unit = {},
) {
    val uniqueVideos = remember(videos) {
        videos.distinctBy { it.serviceId to it.id.value }
    }
    var focusedVideo by remember(uniqueVideos) { androidx.compose.runtime.mutableStateOf(uniqueVideos.firstOrNull()) }
    var hasRailFocus by remember { androidx.compose.runtime.mutableStateOf(false) }
    val rowRequester = remember { BringIntoViewRequester() }
    val transitionMillis = LocalTvAppearance.current.transitionMillis
    LaunchedEffect(focusedVideo?.id, hasRailFocus) {
        if (revealFocusedDetails && hasRailFocus) {
            delay(transitionMillis.toLong())
            rowRequester.bringIntoView()
        }
    }
    Column(
        modifier = Modifier.bringIntoViewRequester(rowRequester),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 58.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (cinematic) Color.White else MaterialTheme.colorScheme.onBackground,
        )
        LazyRow(
            modifier = Modifier
                .then(if (revealFocusedDetails) Modifier.height(202.dp) else Modifier)
                .focusRestorer(),
            horizontalArrangement = Arrangement.spacedBy(if (revealFocusedDetails) (-58).dp else 18.dp),
            contentPadding = PaddingValues(horizontal = 58.dp, vertical = 10.dp),
        ) {
            items(uniqueVideos, key = { "${it.serviceId}:${it.id.value}" }) { video ->
                val key = videoFocusKey(title, video)
                val legacyKey = "$title\u001F${video.id.value}"
                val shouldRestore = focusActive && (restoreFocusKey == key || restoreFocusKey == legacyKey)
                CinematicVideoCard(
                    video = video,
                    progressMilliseconds = progressByVideoId[video.id.value],
                    restoreFocus = shouldRestore,
                    onFocused = {
                        hasRailFocus = true
                        focusedVideo = video
                        onFocused(key)
                        onPreviewVideo(video)
                    },
                    onClick = { onOpenVideo(video) },
                    cinematic = cinematic,
                    expandedFocus = revealFocusedDetails,
                )
            }
        }
        if (revealFocusedDetails && hasRailFocus) {
            RailFocusSummary(focusedVideo, cinematic)
        }
    }
}

@Composable
internal fun CinematicVideoCard(
    video: Video,
    progressMilliseconds: Long?,
    restoreFocus: Boolean = false,
    onFocused: () -> Unit = {},
    onClick: () -> Unit,
    cinematic: Boolean = true,
    expandedFocus: Boolean = false,
) {
    val appearance = LocalTvAppearance.current
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    val focused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            focused && expandedFocus -> 1.5f
            focused -> 1.075f
            else -> 1f
        },
        animationSpec = tween(appearance.transitionMillis),
        label = "video-card-scale",
    )
    LaunchedEffect(restoreFocus) {
        if (restoreFocus) focusRequester.requestFocus()
    }
    Column(
        modifier = Modifier
            .width(if (expandedFocus) 278.dp else 184.dp)
            .zIndex(if (focused) 1f else 0f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(if (expandedFocus) 156.dp else 104.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .width(184.dp)
                    .height(104.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .focusRequester(focusRequester)
                    .onFocusChanged { if (it.isFocused) onFocused() }
                    .border(
                        BorderStroke(
                            if (focused) 3.dp else 0.dp,
                            if (focused) {
                                if (cinematic) Color.White else MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            },
                        ),
                        RoundedCornerShape(8.dp),
                    ),
                onClick = onClick,
                interactionSource = interactionSource,
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            ) {
                VideoCardArtwork(video, progressMilliseconds)
            }
        }
        Text(
            video.title,
            modifier = Modifier.width(184.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
            color = if (cinematic) {
                if (focused) Color.White else Color.White.copy(alpha = .72f)
            } else if (focused) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun VideoCardArtwork(video: Video, progressMilliseconds: Long?) {
    Box {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = video.title,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
        if (video.isLive) {
            Text(
                "LIVE",
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                    .background(Color(0xFFE50914), RoundedCornerShape(4.dp)).padding(horizontal = 7.dp, vertical = 3.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        } else if (video.durationSeconds > 0L) {
            Text(
                formatDuration(video.durationSeconds),
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                    .background(Color.Black.copy(alpha = .78f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        val progress = progressMilliseconds?.takeIf { video.durationSeconds > 0L }
            ?.let { (it.toFloat() / (video.durationSeconds * 1_000L)).coerceIn(0f, 1f) }
        if (progress != null) {
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = .24f)))
            Box(
                Modifier.align(Alignment.BottomStart).fillMaxWidth(progress).height(4.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3_600
    val minutes = (seconds % 3_600) / 60
    val remaining = seconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, remaining) else "%d:%02d".format(minutes, remaining)
}

internal fun videoFocusKey(title: String, video: Video): String =
    "$title\u001F${video.serviceId}\u001F${video.id.value}"
