package dev.typetype.android.feature.subscriptions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.typetype.android.R

@Composable
internal fun SubscriptionsHeader(
    selectedTab: SubscriptionsTab,
    channelCount: Int,
    onTabSelect: (SubscriptionsTab) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.subscriptions_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.subscriptions_channel_count,
                    channelCount,
                    channelCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SubscriptionsViewMenu(selectedTab = selectedTab, onTabSelect = onTabSelect)
    }
}

@Composable
private fun SubscriptionsViewMenu(
    selectedTab: SubscriptionsTab,
    onTabSelect: (SubscriptionsTab) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = stringResource(selectedTab.labelResource())
    val viewDescription = stringResource(R.string.subscriptions_view)
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.semantics {
                contentDescription = viewDescription
                stateDescription = selectedLabel
            },
        ) {
            Icon(Icons.Outlined.VideoLibrary, contentDescription = null)
            Text(selectedLabel, modifier = Modifier.padding(start = 6.dp))
            Icon(Icons.Default.ExpandMore, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SubscriptionsTab.entries.forEach { tab ->
                DropdownMenuItem(
                    text = { Text(stringResource(tab.labelResource())) },
                    modifier = Modifier.semantics { selected = tab == selectedTab },
                    onClick = {
                        expanded = false
                        onTabSelect(tab)
                    },
                    leadingIcon = if (tab == selectedTab) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

private fun SubscriptionsTab.labelResource(): Int = when (this) {
    SubscriptionsTab.Videos -> R.string.subscriptions_tab_videos
    SubscriptionsTab.Channels -> R.string.subscriptions_tab_channels
}
