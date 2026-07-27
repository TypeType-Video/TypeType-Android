package dev.typetype.android.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.typetype.android.R
import dev.typetype.android.domain.feed.VideoAvailability

@Composable
internal fun VideoAvailabilityDialog(
    availability: VideoAvailability,
    onDismiss: () -> Unit,
) {
    val title = when (availability) {
        VideoAvailability.Scheduled -> stringResource(R.string.video_scheduled_title)
        VideoAvailability.MembersOnly -> stringResource(R.string.video_members_only_title)
        VideoAvailability.Playable -> return
    }
    val message = when (availability) {
        VideoAvailability.Scheduled -> stringResource(R.string.video_scheduled_message)
        VideoAvailability.MembersOnly -> stringResource(R.string.state_member_only_message)
        VideoAvailability.Playable -> return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_ok))
            }
        },
    )
}
