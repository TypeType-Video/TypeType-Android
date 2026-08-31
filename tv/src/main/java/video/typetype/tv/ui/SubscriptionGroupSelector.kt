package video.typetype.tv.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Border
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import video.typetype.sdk.core.SubscriptionGroup

@Composable
internal fun SubscriptionGroupSelector(
    groups: List<SubscriptionGroup>,
    selectedGroupId: String?,
    enabled: Boolean,
    onSelect: (String?) -> Unit,
) {
    val options = listOf<SubscriptionGroup?>(null) + groups
    LazyRow(
        modifier = Modifier.focusRestorer(),
        contentPadding = PaddingValues(horizontal = 58.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(options, key = { it?.id ?: "all" }) { group ->
            val selected = group?.id == selectedGroupId
            val shape = RoundedCornerShape(8.dp)
            Surface(
                onClick = { onSelect(group?.id) },
                enabled = enabled,
                shape = ClickableSurfaceDefaults.shape(shape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.onSurface,
                    focusedContentColor = MaterialTheme.colorScheme.surface,
                ),
                border = ClickableSurfaceDefaults.border(
                    border = Border(BorderStroke(1.dp, MaterialTheme.colorScheme.border.copy(alpha = .5f)), shape = shape),
                    focusedBorder = Border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), shape = shape),
                ),
            ) {
                Text(
                    text = group?.let { "${it.name}  ${it.channelCount}" } ?: "All",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
