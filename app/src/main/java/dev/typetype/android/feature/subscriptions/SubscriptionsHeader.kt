package dev.typetype.android.feature.subscriptions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.typetype.android.R

@Composable
internal fun SubscriptionsHeader(
    selectedTab: SubscriptionsTab,
    channelCount: Int,
    onTabSelect: (SubscriptionsTab) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(
            text = stringResource(R.string.subscriptions_title),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = pluralStringResource(
                R.plurals.subscriptions_channel_count,
                channelCount,
                channelCount,
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
            SubscriptionsTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { onTabSelect(tab) },
                    text = {
                        Text(
                            stringResource(
                                if (tab == SubscriptionsTab.Videos) {
                                    R.string.subscriptions_tab_videos
                                } else {
                                    R.string.subscriptions_tab_channels
                                },
                            ),
                        )
                    },
                )
            }
        }
    }
}
