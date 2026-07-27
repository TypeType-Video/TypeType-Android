package dev.typetype.android.feature.library.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.typetype.android.R

@Composable
internal fun PlaylistPlaybackActions(
    enabled: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onPlayAll,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        ) {
            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
            Text(
                text = stringResource(R.string.playlist_play_all),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        OutlinedButton(
            onClick = onShuffle,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        ) {
            Icon(imageVector = Icons.Filled.Shuffle, contentDescription = null)
            Text(
                text = stringResource(R.string.playlist_shuffle),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
