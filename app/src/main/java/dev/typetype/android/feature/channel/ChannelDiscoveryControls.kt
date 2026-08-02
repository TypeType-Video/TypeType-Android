package dev.typetype.android.feature.channel

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.domain.channel.ChannelSort

@Composable
internal fun ChannelDiscoveryControls(
    state: ChannelState,
    onAction: (ChannelAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.supportsYouTubeDiscovery) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ChannelTabs(state.tab) { onAction(ChannelAction.OnSelectTab(it)) }
        if (state.tab == ChannelTab.Videos) {
            ChannelSearchField(state, onAction)
            if (state.appliedSearch.isEmpty()) {
                ChannelSortControls(state.sort) { onAction(ChannelAction.OnSelectSort(it)) }
            } else {
                ChannelSearchSummary(state.appliedSearch) { onAction(ChannelAction.OnClearSearch) }
            }
        }
    }
}

@Composable
private fun ChannelTabs(selected: ChannelTab, onSelect: (ChannelTab) -> Unit) {
    val tabs = ChannelTab.entries
    SecondaryScrollableTabRow(selectedTabIndex = tabs.indexOf(selected), edgePadding = 0.dp) {
        tabs.forEach { tab ->
            Tab(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                text = { Text(stringResource(tab.labelResource())) },
            )
        }
    }
}

@Composable
private fun ChannelSearchField(state: ChannelState, onAction: (ChannelAction) -> Unit) {
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onAction(ChannelAction.OnSubmitSearch)
    }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        OutlinedTextField(
            value = state.searchInput,
            onValueChange = { onAction(ChannelAction.OnSearchInputChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.channel_search_label)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (state.searchInput.isNotEmpty()) {
                    IconButton(onClick = { onAction(ChannelAction.OnClearSearch) }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(R.string.channel_search_clear),
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submit() }),
        )
        TextButton(
            onClick = submit,
            enabled = state.searchInput.trim() != state.appliedSearch,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(stringResource(R.string.channel_search_action))
        }
    }
}

@Composable
private fun ChannelSortControls(selected: ChannelSort, onSelect: (ChannelSort) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChannelSort.entries.forEach { sort ->
            FilterChip(
                selected = selected == sort,
                onClick = { onSelect(sort) },
                label = { Text(stringResource(sort.labelResource())) },
            )
        }
    }
}

@Composable
private fun ChannelSearchSummary(query: String, onClear: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.channel_search_results_for, query), modifier = Modifier.weight(1f))
        TextButton(onClick = onClear) { Text(stringResource(R.string.channel_search_back_to_all)) }
    }
}

private fun ChannelTab.labelResource(): Int = when (this) {
    ChannelTab.Videos -> R.string.channel_tab_videos
    ChannelTab.Live -> R.string.channel_tab_live
    ChannelTab.Playlists -> R.string.channel_tab_playlists
}

private fun ChannelSort.labelResource(): Int = when (this) {
    ChannelSort.Latest -> R.string.channel_sort_newest
    ChannelSort.Popular -> R.string.channel_sort_popular
    ChannelSort.Oldest -> R.string.channel_sort_oldest
}
