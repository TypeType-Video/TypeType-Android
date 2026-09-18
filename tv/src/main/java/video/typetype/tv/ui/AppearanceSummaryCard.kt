package video.typetype.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import video.typetype.sdk.core.UserProfile
import video.typetype.tv.ui.theme.TvAppearance

@Composable
internal fun AppearanceSummaryCard(
    profile: UserProfile?,
    appearance: TvAppearance,
    canEditProfile: Boolean,
    firstActionFocus: FocusRequester,
    returnFocus: FocusRequester,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier.width(264.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .92f), shape)
            .border(1.dp, MaterialTheme.colorScheme.border.copy(alpha = .55f), shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Your TypeType", style = MaterialTheme.typography.titleMedium)
        AccountHeader(profile, avatarSize = 48.dp, compact = true)
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.border.copy(alpha = .45f)))
        AppearanceSummaryValue("Personality", appearance.personality.name)
        AppearanceSummaryValue("Palette", appearance.colorTheme.name)
        AppearanceSummaryValue("Motion", appearance.motion.name)
        if (canEditProfile) {
            Button(
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth().focusRequester(firstActionFocus)
                    .focusProperties { left = returnFocus },
                colors = summaryButtonColors(),
            ) {
                SummaryButtonLabel("Edit profile")
            }
        }
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().then(
                if (canEditProfile) Modifier else Modifier.focusRequester(firstActionFocus),
            ).focusProperties { left = returnFocus },
            colors = summaryButtonColors(),
        ) {
            SummaryButtonLabel("Sign out")
        }
    }
}

@Composable
private fun SummaryButtonLabel(label: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(label)
    }
}

@Composable
private fun summaryButtonColors() = ButtonDefaults.colors(
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedContainerColor = MaterialTheme.colorScheme.primary,
    focusedContentColor = MaterialTheme.colorScheme.onPrimary,
)

@Composable
private fun AppearanceSummaryValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}
