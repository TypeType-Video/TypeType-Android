package dev.typetype.android.feature.channel

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import dev.typetype.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChannelTopBar(
    state: ChannelState,
    searchExpanded: Boolean,
    onSearchExpandedChange: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onAction: (ChannelAction) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(searchExpanded) {
        if (searchExpanded) focusRequester.requestFocus()
    }
    TopAppBar(
        navigationIcon = {
            IconButton(
                onClick = {
                    if (searchExpanded) {
                        onAction(ChannelAction.OnDismissSearch)
                        onSearchExpandedChange(false)
                    } else {
                        onNavigateBack()
                    }
                },
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.settings_back),
                )
            }
        },
        title = {
            if (searchExpanded) {
                TextField(
                    value = state.searchInput,
                    onValueChange = { onAction(ChannelAction.OnSearchInputChanged(it)) },
                    placeholder = { Text(stringResource(R.string.channel_search_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            onAction(ChannelAction.OnSubmitSearch)
                            onSearchExpandedChange(false)
                        },
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            } else {
                Text(state.channel?.name.orEmpty(), maxLines = 1)
            }
        },
        actions = {
            when {
                searchExpanded && state.searchInput.isNotEmpty() -> IconButton(
                    onClick = { onAction(ChannelAction.OnClearSearchInput) },
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = stringResource(R.string.channel_search_clear),
                    )
                }
                !searchExpanded && state.channel != null &&
                    state.supportsYouTubeDiscovery && state.tab == ChannelTab.Videos -> {
                    IconButton(onClick = { onSearchExpandedChange(true) }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = stringResource(R.string.channel_search_label),
                        )
                    }
                }
            }
        },
    )
}
