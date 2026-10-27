package dev.typetype.android.feature.settings.updates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.feature.settings.SettingsDetailTopBar

@Composable
fun AppUpdatesScreen(
    onNavigateBack: () -> Unit,
    viewModel: AppUpdatesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsDetailTopBar(title = stringResource(R.string.updates_title), onNavigateBack = onNavigateBack)
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item(key = "status") {
                    AppUpdateStatusCard(
                        versionName = viewModel.versionName,
                        state = state,
                        onCheck = viewModel::check,
                        onDownload = viewModel::download,
                        onInstall = viewModel::install,
                    )
                }
                item(key = "channel") {
                    AppUpdateChannelGroup(
                        selected = state.channel,
                        enabled = state.status != AppUpdateUiStatus.DOWNLOADING,
                        onSelect = viewModel::selectChannel,
                    )
                }
                val release = state.release
                if (release != null && state.status.showsReleaseNotes() && release.releaseNotes.isNotBlank()) {
                    item(key = "notes") { ReleaseNotesCard(release) }
                }
                item(key = "note") {
                    Text(
                        stringResource(R.string.updates_verified_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

private fun AppUpdateUiStatus.showsReleaseNotes(): Boolean =
    this != AppUpdateUiStatus.NEWER_BUILD_INSTALLED && this != AppUpdateUiStatus.UP_TO_DATE
