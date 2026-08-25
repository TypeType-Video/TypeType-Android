package dev.typetype.android.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import dev.typetype.android.core.ui.theme.LocalTypeTypeAppearance

@Composable
fun TypeTypeCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(24.dp),
    content: @Composable () -> Unit,
) {
    val appearance = LocalTypeTypeAppearance.current
    Card(
        modifier = modifier.fillMaxWidth().rotate(if (appearance.isManga && appearance.panelTilt) -0.5f else 0f),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(if (appearance.isManga) 2.dp else 1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box {
            if (appearance.isManga && appearance.screentone) {
                val tone = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                Canvas(Modifier.matchParentSize()) {
                    val step = 9.dp.toPx()
                    var x = step / 2f
                    while (x < size.width) {
                        var y = step / 2f
                        while (y < size.height) {
                            drawCircle(tone, 0.8.dp.toPx(), Offset(x, y))
                            y += step
                        }
                        x += step
                    }
                }
            }
            Column(modifier = Modifier.padding(contentPadding), content = { content() })
        }
    }
}
