package dev.typetype.android.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.typetype.android.R

sealed interface VideoMenuAction {
    data object AddToFavorites : VideoMenuAction
    data object AddToWatchLater : VideoMenuAction
    data object Share : VideoMenuAction
    data object OpenChannel : VideoMenuAction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCardMenu(
    onAction: (VideoMenuAction) -> Unit,
    onDismiss: () -> Unit,
    showOpenChannel: Boolean = true,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            VideoMenuItem(
                icon = Icons.Filled.FavoriteBorder,
                label = stringResource(R.string.video_menu_add_to_favorites),
                onClick = { onAction(VideoMenuAction.AddToFavorites); onDismiss() },
            )
            VideoMenuItem(
                icon = Icons.Filled.BookmarkAdd,
                label = stringResource(R.string.video_menu_add_to_watch_later),
                onClick = { onAction(VideoMenuAction.AddToWatchLater); onDismiss() },
            )
            VideoMenuItem(
                icon = Icons.Filled.Share,
                label = stringResource(R.string.video_menu_share),
                onClick = { onAction(VideoMenuAction.Share); onDismiss() },
            )
            if (showOpenChannel) {
                VideoMenuItem(
                    icon = Icons.Filled.Person,
                    label = stringResource(R.string.video_menu_open_channel),
                    onClick = { onAction(VideoMenuAction.OpenChannel); onDismiss() },
                )
            }
        }
    }
}

@Composable
private fun VideoMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
