package video.typetype.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import video.typetype.sdk.core.SearchFilterOption
import video.typetype.sdk.core.SearchFilters

@Composable
internal fun SearchBar(
    value: String,
    onSubmitQuery: (String) -> Unit,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
) {
    var editing by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val searchButtonFocus = remember { FocusRequester() }
    val submit = { if (value.isNotBlank()) onSubmitQuery(value.trim()) }
    Row(modifier = Modifier.widthIn(max = 720.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier.weight(1f)
                .focusRequester(focusRequester)
                .focusProperties { up = upFocusRequester }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                        upFocusRequester.requestFocus()
                        true
                    } else {
                        false
                    }
                }
                .border(
                    if (focused) 3.dp else 1.dp,
                    if (focused) Color.White else MaterialTheme.colorScheme.border,
                    RoundedCornerShape(8.dp),
                ),
            onClick = { editing = true },
            interactionSource = interactionSource,
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .9f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 15.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    value.ifBlank { "Videos, channels and playlists" },
                    style = MaterialTheme.typography.titleLarge,
                    color = if (value.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
        Button(onClick = submit, enabled = value.isNotBlank(), modifier = Modifier.focusRequester(searchButtonFocus)) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Search")
        }
    }
    if (editing) {
        TvTextPrompt(
            title = "Search TypeType",
            initialValue = value,
            actionLabel = "Search",
            onDismiss = { editing = false },
            onSubmit = {
                editing = false
                onSubmitQuery(it)
            },
        )
    }
}

@Composable
internal fun SearchSuggestionRow(suggestions: List<String>, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Suggestions",
            modifier = Modifier.padding(horizontal = 58.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 58.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(suggestions, key = { it }) { suggestion ->
                Surface(
                    onClick = { onSelect(suggestion) },
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.primary,
                        focusedContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(18.dp)),
                ) { Text(suggestion, modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp)) }
            }
        }
    }
}

@Composable
internal fun SearchFilterRows(
    filters: SearchFilters,
    selectedContent: String?,
    selectedSort: String?,
    selectedGroups: Map<String, List<String>>,
    onContent: (String?) -> Unit,
    onSort: (String?) -> Unit,
    onGroup: (String, String, Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (filters.contentFilters.isNotEmpty()) {
            FilterRow("Type", filters.contentFilters, selectedContent, onContent)
        }
        if (filters.sortFilters.isNotEmpty()) {
            FilterRow("Sort", filters.sortFilters, selectedSort, onSort)
        }
        filters.groups.forEach { group ->
            FilterRow(group.label, group.options, selectedGroups[group.key].orEmpty()) { value ->
                onGroup(group.key, value, group.multiSelect)
            }
        }
    }
}

@Composable
private fun FilterRow(
    title: String,
    options: List<SearchFilterOption>,
    selected: String?,
    onSelect: (String?) -> Unit,
) = FilterRow(title, options, selected?.let(::listOf).orEmpty()) { value ->
    onSelect(value.takeUnless { it == selected })
}

@Composable
private fun FilterRow(
    title: String,
    options: List<SearchFilterOption>,
    selected: List<String>,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(title, modifier = Modifier.padding(horizontal = 58.dp), fontWeight = FontWeight.SemiBold)
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 58.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(options, key = { it.value }) { option ->
                val active = option.value in selected || selected.isEmpty() && option.isDefault
                Surface(
                    onClick = { onSelect(option.value) },
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
                ) { Text(option.label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)) }
            }
        }
    }
}
