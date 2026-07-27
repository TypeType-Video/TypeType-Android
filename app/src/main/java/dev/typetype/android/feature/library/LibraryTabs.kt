package dev.typetype.android.feature.library

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.typetype.android.R

@Composable
fun LibraryTabs(
    selectedTab: LibraryTab,
    onTabSelect: (LibraryTab) -> Unit,
) {
    val tabs = LibraryTab.entries
    SecondaryScrollableTabRow(
        selectedTabIndex = tabs.indexOf(selectedTab),
        containerColor = MaterialTheme.colorScheme.background,
        edgePadding = 0.dp,
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelect(tab) },
                text = {
                    Text(
                        text = when (tab) {
                            LibraryTab.History -> stringResource(R.string.library_tab_history)
                            LibraryTab.Favorites -> stringResource(R.string.library_tab_favorites)
                            LibraryTab.WatchLater -> stringResource(R.string.library_tab_watch_later)
                            LibraryTab.Playlists -> stringResource(R.string.library_tab_playlists)
                            LibraryTab.SavedPlaylists -> stringResource(R.string.library_tab_saved_playlists)
                        },
                    )
                },
            )
        }
    }
}
