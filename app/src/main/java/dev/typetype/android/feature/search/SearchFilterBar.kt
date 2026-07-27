package dev.typetype.android.feature.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.domain.search.SearchFilterOption

@Composable
fun SearchFilterBar(
    contentFilters: List<SearchFilterOption>,
    sortFilters: List<SearchFilterOption>,
    selectedContent: String?,
    selectedSort: String?,
    onContentSelect: (String?) -> Unit,
    onSortSelect: (String?) -> Unit,
) {
    if (contentFilters.isEmpty() && sortFilters.isEmpty()) return
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (contentFilters.isNotEmpty()) {
            FilterRow(
                options = contentFilters.filterNot { it.label.equals("all", ignoreCase = true) },
                defaultLabel = stringResource(R.string.search_filter_all),
                selected = selectedContent,
                onSelect = onContentSelect,
            )
        }
        if (sortFilters.isNotEmpty()) {
            FilterRow(
                options = sortFilters,
                defaultLabel = stringResource(R.string.search_filter_relevance),
                selected = selectedSort,
                onSelect = onSortSelect,
            )
        }
    }
}

@Composable
private fun FilterRow(
    options: List<SearchFilterOption>,
    defaultLabel: String,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(defaultLabel) },
        )
        options.forEach { option ->
            FilterChip(
                selected = selected == option.value,
                onClick = { onSelect(option.value) },
                label = { Text(prettifyFilterLabel(option.label)) },
            )
        }
    }
}

private fun prettifyFilterLabel(raw: String): String {
    val value = raw.substringAfter(':').trim().removePrefix("sort_")
    return value.split('_', ' ')
        .filter(String::isNotBlank)
        .joinToString(" ") { word -> word.lowercase().replaceFirstChar(Char::titlecase) }
}
