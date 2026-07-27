package dev.typetype.android.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import dev.typetype.android.R

@Composable
fun VideoMoreActionsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    overlay: Boolean = false,
) {
    val styledModifier = if (overlay) {
        modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.72f))
    } else {
        modifier
    }
    IconButton(onClick = onClick, modifier = styledModifier) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = stringResource(R.string.video_more_actions),
            tint = if (overlay) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
