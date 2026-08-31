package video.typetype.tv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import video.typetype.sdk.core.UserProfile
import video.typetype.tv.data.TvProfileActions

private enum class ProfileField {
    Username,
    Bio,
    Avatar,
}

@Composable
internal fun ProfileEditorDialog(
    profile: UserProfile?,
    isActionInProgress: Boolean,
    actions: TvProfileActions,
    onDismiss: () -> Unit,
) {
    var field by remember { mutableStateOf<ProfileField?>(null) }
    when (field) {
        ProfileField.Username -> ProfilePrompt(
            "Display name", profile?.publicUsername.orEmpty(), true,
            { field = null },
        ) { actions.update(it, profile?.bio); field = null }
        ProfileField.Bio -> ProfilePrompt(
            "Bio", profile?.bio.orEmpty(), true,
            { field = null },
        ) { actions.update(profile?.publicUsername, it); field = null }
        ProfileField.Avatar -> ProfilePrompt(
            "Emoji avatar code", profile?.avatarCode.orEmpty(), false,
            { field = null },
        ) { actions.setEmojiAvatar(it); field = null }
        null -> ProfileActionsDialog(profile, isActionInProgress, { field = it }, actions.clearAvatar, onDismiss)
    }
}

@Composable
private fun ProfilePrompt(
    title: String,
    initialValue: String,
    allowBlank: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    TvTextPrompt(
        title = title,
        initialValue = initialValue,
        allowBlank = allowBlank,
        actionLabel = "Save",
        onDismiss = onDismiss,
        onSubmit = onSubmit,
    )
}

@Composable
private fun ProfileActionsDialog(
    profile: UserProfile?,
    busy: Boolean,
    onField: (ProfileField) -> Unit,
    onClearAvatar: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), modifier = Modifier.width(660.dp)) {
            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("Edit profile", style = MaterialTheme.typography.headlineMedium)
                AccountHeader(profile)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { onField(ProfileField.Username) }, enabled = !busy, colors = editorButtonColors()) { Text("Display name") }
                    Button(onClick = { onField(ProfileField.Bio) }, enabled = !busy, colors = editorButtonColors()) { Text("Bio") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { onField(ProfileField.Avatar) }, enabled = !busy, colors = editorButtonColors()) { Text("Emoji avatar") }
                    Button(
                        onClick = onClearAvatar,
                        enabled = !busy && profile?.avatarType != null,
                        colors = editorButtonColors(),
                    ) { Text("Remove avatar") }
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    colors = editorButtonColors(),
                ) { Text("Done") }
            }
        }
    }
}

@Composable
private fun editorButtonColors() = ButtonDefaults.colors(
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedContainerColor = MaterialTheme.colorScheme.primary,
    focusedContentColor = MaterialTheme.colorScheme.onPrimary,
)
