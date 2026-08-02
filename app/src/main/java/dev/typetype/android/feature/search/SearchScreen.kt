package dev.typetype.android.feature.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.AnimatedError

@Composable
fun SearchRoute(
    onNavigateBack: () -> Unit,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenChannel: (channelUrl: String) -> Unit = {},
    onOpenPlaylist: (playlistUrl: String) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SearchScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onPlayVideo = onPlayVideo,
        onOpenChannel = onOpenChannel,
        onOpenPlaylist = onOpenPlaylist,
        onAction = viewModel::onAction,
    )
}

@Composable
fun SearchScreen(
    state: SearchState,
    onNavigateBack: () -> Unit,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenChannel: (channelUrl: String) -> Unit,
    onOpenPlaylist: (playlistUrl: String) -> Unit,
    onAction: (SearchAction) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    fun submit() {
        keyboardController?.hide()
        focusManager.clearFocus()
        onAction(SearchAction.OnSearch)
    }

    fun submitTerm(term: String) {
        keyboardController?.hide()
        focusManager.clearFocus()
        onAction(SearchAction.OnSuggestionClick(term))
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        SearchTopBar(
            query = state.query,
            onQueryChange = { onAction(SearchAction.OnQueryChange(it)) },
            onSubmit = ::submit,
            onClear = { onAction(SearchAction.OnClearQuery) },
            onNavigateBack = onNavigateBack,
            focusRequester = focusRequester,
        )
        SearchFilterBar(
            contentFilters = state.contentFilters,
            filterGroups = state.filterGroups,
            selectedContent = state.selectedContentFilter,
            selectedFilters = state.selectedFilters,
            onContentSelect = { onAction(SearchAction.OnContentFilterSelect(it)) },
            onFilterToggle = { groupKey, value ->
                onAction(SearchAction.OnFilterToggle(groupKey, value))
            },
            onResetFilters = { onAction(SearchAction.OnResetFilters) },
        )

        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            state.errorMessage != null -> AnimatedError(
                message = state.errorMessage,
                requestId = state.errorRequestId,
                onRetry = { onAction(SearchAction.OnSearch) },
            )
            !state.hasSearched -> SuggestionsAndHistory(
                query = state.query,
                suggestions = state.suggestions,
                history = state.searchHistory,
                onSuggestionClick = ::submitTerm,
                onSuggestionFill = { onAction(SearchAction.OnQueryChange(it)) },
                onHistoryClick = ::submitTerm,
                onDeleteHistory = { onAction(SearchAction.OnDeleteHistoryEntry(it)) },
            )
            else -> SearchResultsGrid(
                state = state,
                onPlayVideo = onPlayVideo,
                onOpenChannel = onOpenChannel,
                onOpenPlaylist = onOpenPlaylist,
                onSearchSuggestion = { onAction(SearchAction.OnSuggestionClick(it)) },
                onLoadMore = { onAction(SearchAction.OnLoadMore) },
            )
        }
    }
}

@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    onNavigateBack: () -> Unit,
    focusRequester: FocusRequester,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.search_back),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = 14.dp, end = 4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    )
                }
                AnimatedVisibility(visible = query.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.search_clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(4.dp))
        IconButton(
            onClick = onSubmit,
            enabled = query.trim().isNotEmpty(),
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.search_submit),
                tint = if (query.trim().isNotEmpty()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
