package dev.typetype.android.feature.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.domain.search.SearchFilterGroup
import dev.typetype.android.domain.search.SearchFilterOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFilterBar(
    contentFilters: List<SearchFilterOption>,
    filterGroups: List<SearchFilterGroup>,
    selectedContent: String?,
    selectedFilters: List<String>,
    onContentSelect: (String?) -> Unit,
    onFilterToggle: (String, String) -> Unit,
    onResetFilters: () -> Unit,
) {
    var sheetOpen by remember { mutableStateOf(false) }
    val activeOptions = activeSearchFilterOptions(filterGroups, selectedFilters)
    val contentOptions = contentFilters.filterNot {
        it.isDefault || prettifyFilterLabel(it.label).equals("all", ignoreCase = true)
    }
    val selectedContentLabel = contentOptions.firstOrNull { it.value == selectedContent }
        ?.let { prettifyFilterLabel(it.label) }
        ?: stringResource(R.string.search_filter_all)
    if (contentOptions.isEmpty() && filterGroups.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchContentMenu(
                label = selectedContentLabel,
                options = contentOptions,
                selectedContent = selectedContent,
                onSelect = onContentSelect,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (filterGroups.isNotEmpty()) {
                OutlinedButton(onClick = { sheetOpen = true }) {
                    Text(
                        if (activeOptions.isEmpty()) stringResource(R.string.search_filters)
                        else stringResource(R.string.search_filters_active, activeOptions.size),
                    )
                }
            }
        }
        if (activeOptions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                activeOptions.forEach { option ->
                    val group = filterGroups.first { option in it.options }
                    InputChip(
                        selected = true,
                        onClick = { onFilterToggle(group.key, option.value) },
                        label = { Text(prettifyFilterLabel(option.label)) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(
                                    R.string.search_filter_remove,
                                    prettifyFilterLabel(option.label),
                                ),
                            )
                        },
                    )
                }
            }
        }
    }

    if (sheetOpen) {
        ModalBottomSheet(onDismissRequest = { sheetOpen = false }) {
            SearchFiltersSheet(
                groups = filterGroups,
                selected = selectedFilters,
                onToggle = onFilterToggle,
                onReset = onResetFilters,
            )
        }
    }
}

@Composable
private fun SearchContentMenu(
    label: String,
    options: List<SearchFilterOption>,
    selectedContent: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val accessibilityLabel = stringResource(R.string.search_content_type)
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.semantics {
                contentDescription = accessibilityLabel
                stateDescription = label
            },
        ) {
            Icon(Icons.Outlined.VideoLibrary, contentDescription = null)
            Text(label, modifier = Modifier.padding(start = 6.dp))
            Icon(Icons.Default.ExpandMore, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SearchContentMenuItem(
                label = stringResource(R.string.search_filter_all),
                selected = selectedContent == null,
            ) {
                expanded = false
                onSelect(null)
            }
            options.forEach { option ->
                SearchContentMenuItem(
                    label = prettifyFilterLabel(option.label),
                    selected = selectedContent == option.value,
                ) {
                    expanded = false
                    onSelect(option.value)
                }
            }
        }
    }
}

@Composable
private fun SearchContentMenuItem(label: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null) }
        } else {
            null
        },
    )
}

@Composable
private fun SearchFiltersSheet(
    groups: List<SearchFilterGroup>,
    selected: List<String>,
    onToggle: (String, String) -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.search_filters),
                fontWeight = FontWeight.SemiBold,
            )
            if (selected.isNotEmpty()) {
                TextButton(onClick = onReset) { Text(stringResource(R.string.search_filters_reset)) }
            }
        }
        groups.forEach { group ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = prettifyFilterLabel(group.label),
                    fontWeight = FontWeight.Medium,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    group.options.forEach { option ->
                        FilterChip(
                            selected = isSearchFilterSelected(group, option, selected),
                            onClick = { onToggle(group.key, option.value) },
                            label = { Text(prettifyFilterLabel(option.label)) },
                        )
                    }
                }
            }
        }
    }
}

internal fun prettifyFilterLabel(raw: String): String {
    val value = raw.substringAfter(':').trim().removePrefix("sort_")
    return value.split('_', ' ')
        .filter(String::isNotBlank)
        .joinToString(" ") { word -> word.lowercase().replaceFirstChar(Char::titlecase) }
}
