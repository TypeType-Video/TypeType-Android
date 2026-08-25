package dev.typetype.android.feature.settings.blocked

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.SectionHeader
import dev.typetype.android.domain.actions.BlockedItem
import dev.typetype.android.domain.actions.BlockedKeyword
import dev.typetype.android.feature.settings.SettingsDetailTopBar

@Composable
fun BlockedSettingsRoute(
    onNavigateBack: () -> Unit,
    viewModel: BlockedSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsDetailTopBar(
                title = stringResource(R.string.settings_blocked_title),
                onNavigateBack = onNavigateBack,
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { SectionHeader(stringResource(R.string.settings_blocked_section_keywords)) }
                item {
                    KeywordInput(
                        value = state.keywordInput,
                        isAdding = state.isAddingKeyword,
                        error = state.keywordError,
                        onValueChange = viewModel::onKeywordChange,
                        onAdd = viewModel::addKeyword,
                    )
                }
                if (state.keywords.isEmpty()) {
                    item { EmptyRow(stringResource(R.string.settings_blocked_empty_keywords)) }
                } else {
                    items(state.keywords, key = { "keyword-${it.keyword}" }) { item ->
                        BlockedKeywordRow(item, onUnblock = { viewModel.unblockKeyword(item.keyword) })
                    }
                }
                item { Spacer(Modifier.size(8.dp)) }
                item { SectionHeader(stringResource(R.string.settings_blocked_section_channels)) }
                if (state.channels.isEmpty()) {
                    item { EmptyRow(stringResource(R.string.settings_blocked_empty_channels)) }
                } else {
                    items(state.channels, key = { "ch-${it.url}" }) { item ->
                        BlockedRow(
                            item = item,
                            avatarShape = CircleShape,
                            onUnblock = { viewModel.unblockChannel(item.url) },
                        )
                    }
                }
                item { Spacer(Modifier.size(8.dp)) }
                item { SectionHeader(stringResource(R.string.settings_blocked_section_videos)) }
                if (state.videos.isEmpty()) {
                    item { EmptyRow(stringResource(R.string.settings_blocked_empty_videos)) }
                } else {
                    items(state.videos, key = { "vid-${it.url}" }) { item ->
                        BlockedRow(
                            item = item,
                            avatarShape = MaterialTheme.shapes.small,
                            onUnblock = { viewModel.unblockVideo(item.url) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeywordInput(
    value: String,
    isAdding: Boolean,
    error: String?,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.settings_blocked_keyword_label)) },
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { message -> ({ Text(message) }) },
            trailingIcon = {
                IconButton(onClick = onAdd, enabled = value.isNotBlank() && !isAdding) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.settings_blocked_keyword_add),
                    )
                }
            },
        )
    }
}

@Composable
private fun BlockedKeywordRow(item: BlockedKeyword, onUnblock: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.keyword,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = onUnblock) {
                Text(
                    text = stringResource(R.string.settings_blocked_unblock),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun EmptyRow(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BlockedRow(
    item: BlockedItem,
    avatarShape: androidx.compose.ui.graphics.Shape,
    onUnblock: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(avatarShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (item.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.name.ifBlank { blockedItemDisplayPath(item.url) },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.name.isNotBlank()) {
                    Text(
                        text = blockedItemDisplayPath(item.url),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            TextButton(onClick = onUnblock) {
                Text(
                    text = stringResource(R.string.settings_blocked_unblock),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
