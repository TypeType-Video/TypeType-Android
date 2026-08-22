package dev.typetype.android.feature.channel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import dev.typetype.android.R
import dev.typetype.android.domain.channel.ChannelSort

@Composable
internal fun ChannelDiscoveryControls(
    state: ChannelState,
    searchExpanded: Boolean,
    onSearchExpandedChange: (Boolean) -> Unit,
    onAction: (ChannelAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.supportsYouTubeDiscovery) return
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChannelMenuButton(
                label = stringResource(state.tab.labelResource()),
                accessibilityLabel = stringResource(R.string.channel_section),
                leadingIcon = { Icon(Icons.Outlined.VideoLibrary, contentDescription = null) },
                options = ChannelTab.entries,
                selectedOption = state.tab,
                optionLabel = { stringResource(it.labelResource()) },
                onSelect = { onAction(ChannelAction.OnSelectTab(it)) },
            )
            if (state.tab == ChannelTab.Videos && state.appliedSearch.isEmpty()) {
                ChannelMenuButton(
                    label = stringResource(state.sort.labelResource()),
                    accessibilityLabel = stringResource(R.string.channel_video_order),
                    leadingIcon = { Icon(Icons.Outlined.FilterList, contentDescription = null) },
                    options = ChannelSort.entries,
                    selectedOption = state.sort,
                    optionLabel = { stringResource(it.labelResource()) },
                    onSelect = { onAction(ChannelAction.OnSelectSort(it)) },
                )
            }
            Spacer(Modifier.weight(1f))
            if (state.tab == ChannelTab.Videos) {
                IconButton(onClick = { onSearchExpandedChange(!searchExpanded) }) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = stringResource(R.string.channel_search_label),
                    )
                }
            }
        }
        if (searchExpanded && state.tab == ChannelTab.Videos) {
            ChannelSearchField(
                query = state.searchInput,
                onQueryChange = { onAction(ChannelAction.OnSearchInputChanged(it)) },
                onSubmit = {
                    onAction(ChannelAction.OnSubmitSearch)
                    onSearchExpandedChange(false)
                },
                onClear = { onAction(ChannelAction.OnClearSearchInput) },
            )
        }
        if (state.appliedSearch.isNotEmpty()) {
            ChannelSearchSummary(state.appliedSearch) { onAction(ChannelAction.OnClearSearch) }
        }
    }
}

@Composable
private fun ChannelSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.channel_search_label)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = stringResource(R.string.channel_search_clear),
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                focusManager.clearFocus()
                onSubmit()
            },
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
    )
}

@Composable
private fun <T> ChannelMenuButton(
    label: String,
    accessibilityLabel: String,
    leadingIcon: @Composable () -> Unit,
    options: List<T>,
    selectedOption: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.semantics {
                contentDescription = accessibilityLabel
                stateDescription = label
            },
        ) {
            leadingIcon()
            Text(label, modifier = Modifier.padding(start = 6.dp))
            Icon(Icons.Default.ExpandMore, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    modifier = Modifier.semantics { selected = option == selectedOption },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                    leadingIcon = if (option == selectedOption) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun ChannelSearchSummary(query: String, onClear: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
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
