package video.typetype.tv.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import video.typetype.sdk.core.Video
import video.typetype.tv.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow

@Composable
internal fun AutoplayCountdownOverlay(
    next: Video,
    remainingSeconds: Int,
    totalSeconds: Int,
    paused: Boolean,
    onPlayNow: () -> Unit,
    onTogglePause: () -> Unit,
    onCancel: () -> Unit,
) {
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(next.id) { playFocus.requestFocus() }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = next.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(Color.Black.copy(alpha = .96f), Color.Black.copy(alpha = .62f), Color.Black.copy(alpha = .18f)),
                ),
            ).background(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = .18f), Color.Transparent, Color.Black.copy(alpha = .78f))),
            ),
        )
        Row(
            modifier = Modifier.align(Alignment.TopStart).padding(start = 58.dp, top = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.ic_typetype), "TypeType", Modifier.size(42.dp))
            Text("TYPETYPE", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(40.dp).size(56.dp),
            onClick = onCancel,
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Black.copy(alpha = .48f),
                contentColor = Color.White,
                focusedContainerColor = Color.White,
                focusedContentColor = Color.Black,
            ),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Close, contentDescription = "Cancel autoplay")
            }
        }
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 72.dp, end = 72.dp, bottom = 74.dp)
                .fillMaxWidth(.66f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "UP NEXT",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Text(
                next.title,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            next.uploaderName.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = .7f))
            }
            Text(
                if (paused) "Autoplay paused" else "Starting in $remainingSeconds seconds",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = .68f),
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    modifier = Modifier.focusRequester(playFocus),
                    onClick = onPlayNow,
                    colors = ButtonDefaults.colors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        focusedContainerColor = MaterialTheme.colorScheme.primary,
                        focusedContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text("Play now", fontWeight = FontWeight.SemiBold)
                }
                Button(onClick = onTogglePause) {
                    Icon(if (paused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text(if (paused) "Resume timer" else "Pause timer")
                }
            }
        }
        val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(5.dp).background(Color.White.copy(alpha = .2f))) {
            Box(
                Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(5.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
