package dev.typetype.android.feature.settings.recommender

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.ChannelAvatar
import dev.typetype.android.core.ui.components.SkeletonImage
import dev.typetype.android.domain.recommendations.InterestGroup
import dev.typetype.android.domain.recommendations.ProfileChannel

@Composable
internal fun InterestCard(
    group: InterestGroup,
    state: RecommenderSettingsState,
    highlighted: Boolean,
    onAction: (RecommenderSettingsAction) -> Unit,
) {
    val preferences = state.preferences
    val ignored = group.id in preferences.ignoredSignals
    val pinned = group.pinned || group.id in preferences.pinnedGroups
    val muted = group.muted || group.id in preferences.mutedGroups
    val fade by animateFloatAsState(if (muted || ignored) 0.5f else 1f, label = "interest")
    val border by animateColorAsState(
        if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "interest-border",
    )
    val status = listOfNotNull(
        if (group.custom) stringResource(R.string.algo_interest_yours) else basedOn(group.basedOn),
        if (pinned && !group.custom) stringResource(R.string.algo_pinned) else null,
        if (muted) stringResource(R.string.algo_muted) else null,
        if (ignored) stringResource(R.string.algo_ignored) else null,
    ).joinToString(" · ")
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(2.dp, border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, top = 6.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f).alpha(fade), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(group.label, style = MaterialTheme.typography.bodyLarge)
                    Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                InterestMenu(group, pinned, muted, ignored, onAction)
            }
            val examples = group.examples.filter { !it.thumbnailUrl.isNullOrBlank() }.take(MAX_EXAMPLES)
            if (examples.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.alpha(fade)) {
                    examples.forEach { example ->
                        SkeletonImage(
                            imageUrl = example.thumbnailUrl.orEmpty(),
                            contentDescription = example.title,
                            modifier = Modifier.width(72.dp).aspectRatio(16f / 9f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InterestMenu(
    group: InterestGroup,
    pinned: Boolean,
    muted: Boolean,
    ignored: Boolean,
    onAction: (RecommenderSettingsAction) -> Unit,
) {
    if (group.custom) {
        IconButton(onClick = { onAction(RecommenderSettingsAction.RemoveInterest(group.label)) }) {
            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.algo_interest_remove_named, group.label))
        }
        return
    }
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.algo_interest_actions, group.label))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (group.pinnable) {
                MenuItem(if (pinned) R.string.algo_unpin else R.string.algo_pin, Icons.Outlined.PushPin) {
                    open = false
                    onAction(RecommenderSettingsAction.TogglePinned(group.id))
                }
            }
            MenuItem(if (muted) R.string.algo_show else R.string.algo_hide, if (muted) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff) {
                open = false
                onAction(RecommenderSettingsAction.ToggleMuted(group.id))
            }
            MenuItem(if (ignored) R.string.algo_count_again else R.string.algo_ignore, Icons.Outlined.Block) {
                open = false
                onAction(RecommenderSettingsAction.ToggleIgnored(group.id))
            }
        }
    }
}

@Composable
private fun MenuItem(label: Int, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
    )
}

@Composable
internal fun ChannelChip(channel: ProfileChannel, ignored: Boolean, onClick: () -> Unit) {
    val fade by animateFloatAsState(if (ignored) 0.4f else 1f, label = "channel")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .width(76.dp)
            .alpha(fade)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        ChannelAvatar(avatarUrl = channel.avatarUrl.orEmpty(), name = channel.name, size = 56.dp)
        Text(
            channel.name,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChannelSheet(channel: ProfileChannel, ignored: Boolean, onToggle: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        ) {
            ChannelAvatar(avatarUrl = channel.avatarUrl.orEmpty(), name = channel.name, size = 72.dp)
            Text(channel.name, style = MaterialTheme.typography.titleMedium)
            Text(basedOn(channel.basedOn), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
                onClick = {
                    onToggle()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(if (ignored) R.string.algo_count_again else R.string.algo_ignore),
                    color = if (ignored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private const val MAX_EXAMPLES = 3
