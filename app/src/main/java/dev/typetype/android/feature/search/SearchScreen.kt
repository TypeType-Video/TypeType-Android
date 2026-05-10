package dev.typetype.android.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.VideoCard

@Composable
fun SearchRoute(
    onNavigateBack: () -> Unit,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenChannel: (channelUrl: String) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SearchScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onPlayVideo = onPlayVideo,
        onOpenChannel = onOpenChannel,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    state: SearchState,
    onNavigateBack: () -> Unit,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenChannel: (channelUrl: String) -> Unit,
    onAction: (SearchAction) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val menuScope = dev.typetype.android.feature.menu.rememberVideoMenuScope(
        onOpenChannel = onOpenChannel,
    )
    val visibleResults = state.results.filterNot(menuScope::isHidden)

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                TextField(
                    value = state.query,
                    onValueChange = { onAction(SearchAction.OnQueryChange(it)) },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onAction(SearchAction.OnSearch) }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.search_back),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            },
            actions = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { onAction(SearchAction.OnClearQuery) }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.search_clear),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )

        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            state.errorMessage != null -> Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            !state.hasSearched -> SearchHistoryList(
                history = state.searchHistory,
                onEntryClick = { onAction(SearchAction.OnHistoryEntryClick(it)) },
                onDeleteEntry = { onAction(SearchAction.OnDeleteHistoryEntry(it)) },
            )
            visibleResults.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.search_no_results, state.query),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(visibleResults, key = { it.id }) { video ->
                    VideoCard(
                        video = video,
                        onClick = { onPlayVideo(video.url) },
                        onChannelClick = { onOpenChannel(video.uploaderUrl) },
                        onMenuAction = { action -> menuScope.onAction(action, video) },
                        menuItemState = menuScope.stateFor(video),
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchHistoryList(
    history: List<String>,
    onEntryClick: (String) -> Unit,
    onDeleteEntry: (String) -> Unit,
) {
    if (history.isEmpty()) return
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                text = stringResource(R.string.search_recent_searches),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        items(history, key = { it }) { query ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEntryClick(query) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = query,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onDeleteEntry(query) }) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = stringResource(R.string.search_clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
