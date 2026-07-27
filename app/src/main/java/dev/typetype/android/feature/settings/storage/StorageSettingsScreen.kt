package dev.typetype.android.feature.settings.storage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.domain.download.DownloadItem

@Composable
fun StorageSettingsRoute(
    onNavigateBack: () -> Unit,
    viewModel: StorageSettingsViewModel = hiltViewModel(),
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val actionFailed = stringResource(R.string.download_action_failed)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event == StorageEvent.ActionFailed) snackbarHostState.showSnackbar(actionFailed)
        }
    }
    StorageSettingsScreen(
        downloads = downloads,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onOpenDownload = viewModel::openDownload,
        onCancelDownload = viewModel::cancelDownload,
        onRetryDownload = viewModel::retryDownload,
        onRemoveDownload = viewModel::removeDownload,
    )
}

@Composable
fun StorageSettingsScreen(
    downloads: List<DownloadItem>,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onOpenDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onRetryDownload: (String) -> Unit,
    onRemoveDownload: (String) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
                StorageTopBar(onNavigateBack = onNavigateBack)
                if (downloads.isEmpty()) {
                    EmptyDownloads(modifier = Modifier.weight(1f))
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        items(downloads, key = { it.requestId }, contentType = { "download" }) { item ->
                            StorageDownloadRow(
                                item = item,
                                onOpen = { onOpenDownload(item.requestId) },
                                onCancel = { onCancelDownload(item.requestId) },
                                onRetry = { onRetryDownload(item.requestId) },
                                onRemove = { onRemoveDownload(item.requestId) },
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.systemBars),
        )
    }
}

@Composable
private fun StorageTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.settings_back),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.offset(y = (-1).dp),
            )
        }
        Text(
            text = stringResource(R.string.settings_storage_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun EmptyDownloads(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Download,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(8.dp),
        )
        Text(
            text = stringResource(R.string.settings_storage_empty_downloads),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
