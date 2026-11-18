package dev.typetype.android.feature.settings.linkedaccounts

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import kotlinx.coroutines.launch

@Composable
internal fun TwitchLinkCard(viewModel: TwitchLinkViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DisposableEffect(viewModel) { onDispose { viewModel.cancel() } }
    if (!state.available) return
    TwitchLinkCardContent(state, viewModel::start, viewModel::cancel, viewModel::disconnect)
}

@Composable
internal fun TwitchLinkCardContent(state: TwitchLinkState, onStart: () -> Unit, onCancel: () -> Unit, onDisconnect: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    LinkedCard(
        title = stringResource(R.string.linked_accounts_twitch),
        status = when {
            state.linked -> state.linkedName?.let { stringResource(R.string.linked_accounts_twitch_connected_as, it) }
                ?: stringResource(R.string.linked_accounts_connected)
            else -> stringResource(R.string.linked_accounts_not_connected)
        },
        error = state.error ?: if (state.expired) stringResource(R.string.linked_accounts_twitch_expired) else null,
    ) {
        val code = state.code
        when {
            code != null -> {
                Text(stringResource(R.string.linked_accounts_twitch_code_hint), style = MaterialTheme.typography.bodyMedium)
                Text(code.userCode, style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { context.openUrl(code.activationUrl) }) {
                        Text(stringResource(R.string.linked_accounts_twitch_open))
                    }
                    OutlinedButton(onClick = {
                        scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("code", code.userCode))) }
                    }) { Text(stringResource(R.string.linked_accounts_copy_code)) }
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
                }
            }
            state.linked -> OutlinedButton(onClick = onDisconnect, enabled = !state.busy) {
                Text(stringResource(R.string.linked_accounts_disconnect))
            }
            else -> Button(onClick = onStart) { Text(stringResource(R.string.linked_accounts_twitch_start)) }
        }
    }
}
