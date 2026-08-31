package video.typetype.tv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import video.typetype.sdk.core.Video

@Composable
internal fun RailFocusSummary(video: Video?, cinematic: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .padding(horizontal = 58.dp),
    ) {
        if (video != null) {
            RailFocusSummaryContent(video, cinematic)
        }
    }
}

@Composable
private fun RailFocusSummaryContent(video: Video, cinematic: Boolean) {
    val primary = if (cinematic) Color.White else MaterialTheme.colorScheme.onBackground
    val secondary = if (cinematic) Color.White.copy(alpha = .72f) else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier.widthIn(max = 760.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            video.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = primary,
        )
        Text(
            listOf(video.uploaderName, video.relativeUploadDate())
                .filter(String::isNotBlank)
                .joinToString("  •  "),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        video.shortDescription?.takeIf(String::isNotBlank)?.let { description ->
            Text(
                description,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = secondary,
            )
        }
    }
}
