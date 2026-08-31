package video.typetype.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import video.typetype.sdk.core.Video

@Composable
internal fun FeaturedShelf(
    selected: Video,
    label: String = "Featured for you",
    playFocusRequester: FocusRequester,
    onPlay: (Video) -> Unit,
    onDetails: (Video) -> Unit,
    onHeroFocused: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(540.dp),
    ) {
        CinematicBackdrop(selected, Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = .48f),
                        .52f to Color.Black.copy(alpha = .12f),
                        1f to Color.Transparent,
                    ),
                ),
        )
        FeaturedMetadata(
            video = selected,
            label = label,
            playFocusRequester = playFocusRequester,
            onPlay = { onPlay(selected) },
            onDetails = { onDetails(selected) },
            onHeroFocused = onHeroFocused,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 58.dp, bottom = 46.dp),
        )
    }
}

@Composable
private fun FeaturedMetadata(
    video: Video,
    label: String,
    playFocusRequester: FocusRequester,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    onHeroFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.widthIn(max = 680.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            video.title,
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 38.sp, lineHeight = 42.sp),
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = Color.White,
        )
        Text(
            listOf(video.uploaderName, video.relativeUploadDate()).filter(String::isNotBlank).joinToString("  •  "),
            color = Color.White.copy(alpha = .74f),
            style = MaterialTheme.typography.titleSmall,
        )
        video.shortDescription?.takeIf(String::isNotBlank)?.let {
            Text(
                it,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.White.copy(alpha = .86f),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                modifier = Modifier.width(148.dp).focusRequester(playFocusRequester)
                    .onFocusChanged { if (it.isFocused) onHeroFocused() },
                onClick = onPlay,
                colors = ButtonDefaults.colors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    focusedContainerColor = MaterialTheme.colorScheme.primary,
                    focusedContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("Play", fontWeight = FontWeight.SemiBold)
            }
            Button(
                modifier = Modifier.width(168.dp).onFocusChanged { if (it.isFocused) onHeroFocused() },
                onClick = onDetails,
            ) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("More info", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
