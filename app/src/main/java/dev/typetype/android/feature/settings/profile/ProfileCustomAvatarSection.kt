package dev.typetype.android.feature.settings.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.RequestIdRow

@Composable
fun ProfileCustomAvatarSection(
    isSaving: Boolean,
    errorKey: String?,
    errorRequestId: String?,
    onSelectFile: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_profile_custom_avatar_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.settings_profile_custom_avatar_helper),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onSelectFile,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 10.dp).size(18.dp),
                    strokeWidth = 2.dp,
                )
            }
            Text(stringResource(R.string.settings_profile_custom_avatar_action))
        }
        avatarErrorMessage(errorKey)?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        errorRequestId?.let { RequestIdRow(requestId = it) }
    }
}

@Composable
private fun avatarErrorMessage(key: String?): String? = when (key) {
    null -> null
    "AVATAR_TOO_LARGE" -> stringResource(R.string.settings_profile_avatar_too_large)
    "AVATAR_FORMAT_UNSUPPORTED" -> stringResource(
        R.string.settings_profile_avatar_format_unsupported,
    )
    "AVATAR_FILE_UNAVAILABLE" -> stringResource(R.string.settings_profile_avatar_file_unavailable)
    "AVATAR_FILE_EMPTY" -> stringResource(R.string.settings_profile_avatar_file_empty)
    else -> stringResource(R.string.settings_profile_avatar_upload_failed)
}
