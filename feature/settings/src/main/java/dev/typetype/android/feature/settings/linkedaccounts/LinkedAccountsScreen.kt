package dev.typetype.android.feature.settings.linkedaccounts

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.domain.linkedsessions.BiliBiliSessionState
import dev.typetype.android.domain.youtubesession.YoutubeSessionStatus
import dev.typetype.android.feature.settings.SettingsDetailTopBar
import kotlinx.coroutines.launch

@Composable
fun LinkedAccountsRoute(onNavigateBack: () -> Unit, viewModel: LinkedAccountsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DisposableEffect(viewModel) {
        onDispose {
            viewModel.cancelYoutube()
            viewModel.cancelBiliBili()
        }
    }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsDetailTopBar(title = stringResource(R.string.linked_accounts_title), onNavigateBack = onNavigateBack)
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f),
            ) {
                item { YoutubeCard(state, viewModel) }
                item { BiliBiliCard(state, viewModel) }
            }
        }
    }
}

@Composable
private fun YoutubeCard(state: LinkedAccountsState, viewModel: LinkedAccountsViewModel) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    LinkedCard(
        title = stringResource(R.string.linked_accounts_youtube),
        status = when (state.youtubeStatus) {
            YoutubeSessionStatus.Connected -> stringResource(R.string.linked_accounts_connected)
            YoutubeSessionStatus.NeedsReconnect -> stringResource(R.string.linked_accounts_needs_reconnect)
            null -> null
            else -> stringResource(R.string.linked_accounts_not_connected)
        },
        error = state.youtubeError,
    ) {
        val login = state.youtubeLogin
        when {
            login != null -> {
                Text(stringResource(R.string.linked_accounts_youtube_code_hint), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = login.userCode,
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { context.openUrl(login.verificationUrl) }) {
                        Text(stringResource(R.string.linked_accounts_youtube_open))
                    }
                    OutlinedButton(onClick = {
                        scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("code", login.userCode))) }
                    }) { Text(stringResource(R.string.linked_accounts_copy_code)) }
                    TextButton(onClick = viewModel::cancelYoutube) { Text(stringResource(R.string.action_cancel)) }
                }
            }
            state.youtubeStatus == YoutubeSessionStatus.Connected -> OutlinedButton(
                onClick = viewModel::disconnectYoutube,
                enabled = !state.busy,
            ) { Text(stringResource(R.string.linked_accounts_disconnect)) }
            else -> Button(onClick = viewModel::startYoutube) {
                Text(stringResource(R.string.linked_accounts_youtube_start))
            }
        }
    }
}

@Composable
private fun BiliBiliCard(state: LinkedAccountsState, viewModel: LinkedAccountsViewModel) {
    val context = LocalContext.current
    LinkedCard(
        title = stringResource(R.string.linked_accounts_bilibili),
        status = when (state.biliBiliState) {
            BiliBiliSessionState.Connected -> stringResource(R.string.linked_accounts_connected)
            BiliBiliSessionState.NeedsReconnect -> stringResource(R.string.linked_accounts_needs_reconnect)
            BiliBiliSessionState.Disconnected -> stringResource(R.string.linked_accounts_not_connected)
            null -> null
        },
        error = state.biliBiliError,
    ) {
        val qr = state.biliBiliQr
        when {
            qr != null -> {
                Text(
                    text = stringResource(
                        if (state.biliBiliScanned) R.string.linked_accounts_bilibili_scanned
                        else R.string.linked_accounts_bilibili_hint,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                QrCodeImage(
                    content = qr.qrUrl,
                    contentDescription = stringResource(R.string.linked_accounts_bilibili_qr),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { context.openUrl(qr.qrUrl) }) {
                        Text(stringResource(R.string.linked_accounts_bilibili_open_app))
                    }
                    TextButton(onClick = viewModel::cancelBiliBili) { Text(stringResource(R.string.action_cancel)) }
                }
            }
            state.biliBiliState == BiliBiliSessionState.Connected -> OutlinedButton(
                onClick = viewModel::disconnectBiliBili,
                enabled = !state.busy,
            ) { Text(stringResource(R.string.linked_accounts_disconnect)) }
            else -> Button(onClick = viewModel::startBiliBili) {
                Text(stringResource(R.string.linked_accounts_bilibili_start))
            }
        }
    }
}

@Composable
private fun LinkedCard(
    title: String,
    status: String?,
    error: String?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            status?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
            error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        }
    }
}

private fun Context.openUrl(url: String) {
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
}
