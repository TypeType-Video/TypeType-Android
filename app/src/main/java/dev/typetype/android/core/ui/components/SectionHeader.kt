package dev.typetype.android.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
