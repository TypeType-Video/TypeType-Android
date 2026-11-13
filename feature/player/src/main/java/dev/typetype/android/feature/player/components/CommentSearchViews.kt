package dev.typetype.android.feature.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.rememberUserErrorMapper

@Composable
internal fun CommentsHeader(search: CommentSearchController, modifier: Modifier = Modifier) {
    var searching by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (searching) {
            CommentSearchField(search = search, modifier = Modifier.weight(1f))
            IconButton(onClick = { searching = false; search.clear() }) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.comments_search_close))
            }
        } else {
            Text(
                text = stringResource(R.string.comments_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { searching = true }) {
                Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.comments_search_open))
            }
        }
    }
}

@Composable
private fun CommentSearchField(search: CommentSearchController, modifier: Modifier) {
    var draft by rememberSaveable { mutableStateOf(search.state.query) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
    OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        singleLine = true,
        placeholder = { Text(stringResource(R.string.comments_search_open)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (draft.isNotEmpty()) {
                IconButton(onClick = { draft = ""; search.clear() }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.comments_search_clear))
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                search.submit(draft)
                focusManager.clearFocus()
            },
        ),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.focusRequester(focusRequester),
    )
}

@Composable
internal fun CommentSearchResults(
    search: CommentSearchController,
    replies: CommentRepliesController,
    onUrlClick: (String) -> Unit,
    onTimestampClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = search.state
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = CommentsListPadding) {
        items(state.comments, key = { it.id }) { comment ->
            CommentRow(
                comment = comment,
                replies = replies,
                onUrlClick = onUrlClick,
                onTimestampClick = onTimestampClick,
            )
            Spacer(Modifier.height(12.dp))
        }
        item(key = "search-footer") {
            CommentSearchFooter(
                state = state,
                onSearchFurther = search::searchFurther,
                onRetry = search::retry,
            )
        }
    }
}

@Composable
private fun CommentSearchFooter(state: CommentSearchState, onSearchFurther: () -> Unit, onRetry: () -> Unit) {
    when {
        state.isLoading -> Column {
            repeat(2) {
                CommentSkeleton()
                Spacer(Modifier.height(16.dp))
            }
        }
        state.failed -> Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = rememberUserErrorMapper().message(state.failure, R.string.comments_search_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            TextButton(onClick = onRetry) { Text(stringResource(R.string.state_retry)) }
        }
        else -> FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        ) {
            Text(
                text = if (state.comments.isEmpty()) {
                    stringResource(R.string.comments_search_none, state.scannedPages)
                } else {
                    stringResource(R.string.comments_search_found, state.comments.size, state.scannedPages)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            if (state.nextCursor != null) {
                FilledTonalButton(onClick = onSearchFurther) {
                    Text(stringResource(R.string.comments_search_more))
                }
            }
        }
    }
}
