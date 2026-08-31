package video.typetype.tv.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.tv.material3.MaterialTheme
import video.typetype.tv.ui.theme.TvAppearance

@Composable
internal fun MangaBackdrop(appearance: TvAppearance) {
    if (!appearance.isManga) return
    val ink = MaterialTheme.colorScheme.onBackground
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (appearance.screentone) {
            var y = 18f
            while (y < size.height) {
                var x = 18f
                while (x < size.width) {
                    drawCircle(ink.copy(alpha = .055f), radius = 1.5f, center = Offset(x, y))
                    x += 34f
                }
                y += 34f
            }
        }
        if (appearance.speedLines) {
            repeat(16) { index ->
                val endY = size.height * (.12f + index * .052f)
                drawLine(
                    color = ink.copy(alpha = .045f),
                    start = Offset(size.width, endY - 90f),
                    end = Offset(size.width * .72f, endY),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round,
                )
            }
        }
        if (appearance.starburst) {
            val center = Offset(size.width * .78f, size.height * .16f)
            repeat(12) { index ->
                val angle = index * (Math.PI * 2 / 12)
                val end = Offset(
                    center.x + kotlin.math.cos(angle).toFloat() * 86f,
                    center.y + kotlin.math.sin(angle).toFloat() * 86f,
                )
                drawLine(ink.copy(alpha = .04f), center, end, strokeWidth = 2f)
            }
        }
    }
}
