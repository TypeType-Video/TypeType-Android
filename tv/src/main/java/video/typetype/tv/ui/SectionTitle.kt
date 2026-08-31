package video.typetype.tv.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import video.typetype.tv.ui.theme.LocalTvAppearance
import video.typetype.tv.ui.theme.TvHeadlineMarker

@Composable
internal fun SectionTitle(title: String) {
    val appearance = LocalTvAppearance.current
    when {
        !appearance.isManga || appearance.headlineMarker == TvHeadlineMarker.None ->
            Text(title, style = MaterialTheme.typography.headlineSmall)
        appearance.headlineMarker == TvHeadlineMarker.Stamp ->
            Text(
                text = title.uppercase(),
                modifier = Modifier.background(
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(5.dp),
                ).padding(horizontal = 12.dp, vertical = 5.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge,
            )
        else -> Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val ink = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.width(58.dp).height(24.dp)) {
                repeat(4) { index ->
                    val y = 4f + index * 6f
                    drawLine(ink, Offset(0f, y), Offset(size.width, y - 7f), 3f, StrokeCap.Round)
                }
            }
            Text(title.uppercase(), style = MaterialTheme.typography.headlineSmall)
        }
    }
}
