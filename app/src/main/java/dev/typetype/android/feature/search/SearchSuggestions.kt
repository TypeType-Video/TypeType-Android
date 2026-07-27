package dev.typetype.android.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.typetype.android.R

@Composable
internal fun SuggestionsAndHistory(
    query: String,
    suggestions: List<String>,
    history: List<String>,
    onSuggestionClick: (String) -> Unit,
    onSuggestionFill: (String) -> Unit,
    onHistoryClick: (String) -> Unit,
    onDeleteHistory: (String) -> Unit,
) {
    val trimmed = query.trim()
    val showSuggestions = trimmed.isNotEmpty() && suggestions.isNotEmpty()
    val showHistory = trimmed.isEmpty() && history.isNotEmpty()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (showSuggestions) {
            items(suggestions, key = { "sug-$it" }) { term ->
                SuggestionRow(
                    term = term,
                    icon = Icons.Filled.Search,
                    onClick = { onSuggestionClick(term) },
                    trailing = {
                        IconButton(onClick = { onSuggestionFill(term) }) {
                            Icon(Icons.Filled.NorthWest, contentDescription = null)
                        }
                    },
                )
            }
        }

        if (showHistory) {
            item(key = "history-header") {
                Text(
                    text = stringResource(R.string.search_recent_searches),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            items(history, key = { "hist-$it" }) { term ->
                SuggestionRow(
                    term = term,
                    icon = Icons.Filled.History,
                    onClick = { onHistoryClick(term) },
                    trailing = {
                        IconButton(onClick = { onDeleteHistory(term) }) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.search_clear),
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    term: String,
    icon: ImageVector,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = term,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}
