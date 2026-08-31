package video.typetype.tv.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

@Composable
internal fun TvLoginField(
    value: String,
    label: String,
    focusRequester: FocusRequester,
    password: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF242832),
            contentColor = Color.White.copy(alpha = .68f),
            focusedContainerColor = Color(0xFF303746),
            focusedContentColor = Color.White,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, Color.White.copy(alpha = .24f)), shape = shape),
            focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = shape),
        ),
    ) {
        Box(Modifier.padding(horizontal = 18.dp, vertical = 15.dp)) {
            Text(
                text = when {
                    value.isEmpty() -> label
                    password -> "*".repeat(value.length.coerceAtMost(24))
                    else -> value
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (value.isEmpty()) FontWeight.Normal else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

internal enum class LoginInput {
    NAME,
    IDENTIFIER,
    PASSWORD,
}
