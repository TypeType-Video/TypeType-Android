package dev.typetype.android.feature.settings.youtubesession

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import dev.typetype.android.core.ui.components.RequestIdRow
import dev.typetype.android.domain.youtubesession.YoutubeRemoteBrowserInput
import dev.typetype.android.feature.settings.SettingsDetailTopBar

@Composable
fun YoutubeSessionRoute(
    onNavigateBack: () -> Unit,
    viewModel: YoutubeSessionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val leave = {
        viewModel.leave()
        onNavigateBack()
    }
    BackHandler(onBack = leave)
    YoutubeSessionScreen(
        state = state,
        onNavigateBack = leave,
        onRefresh = viewModel::refreshStatus,
        onStart = viewModel::startRemoteBrowser,
        onInput = { viewModel.send(it) },
        onCancel = viewModel::cancelRemoteBrowser,
        onDisconnect = viewModel::disconnect,
        onNoticeShown = viewModel::dismissNotice,
    )
}

@Composable
fun YoutubeSessionScreen(
    state: YoutubeSessionState,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
    onStart: () -> Unit,
    onInput: (YoutubeRemoteBrowserInput) -> Unit,
    onCancel: () -> Unit,
    onDisconnect: () -> Unit,
    onNoticeShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val notice = state.notice?.message()
    LaunchedEffect(notice) {
        if (notice != null) {
            snackbarHostState.showSnackbar(notice)
            onNoticeShown()
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                SettingsDetailTopBar(
                    title = stringResource(R.string.youtube_session_settings_title),
                    onNavigateBack = onNavigateBack,
                )
                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    if (maxWidth >= TABLET_BREAKPOINT) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            YoutubeSessionPrimary(
                                state = state,
                                onRefresh = onRefresh,
                                onStart = onStart,
                                onInput = onInput,
                                onCancel = onCancel,
                                modifier = Modifier.weight(1f),
                            )
                            YoutubeSessionStatusCard(
                                state = state,
                                onRefresh = onRefresh,
                                onDisconnect = onDisconnect,
                                modifier = Modifier.width(320.dp),
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            item {
                                YoutubeSessionPrimaryContent(
                                    state = state,
                                    onRefresh = onRefresh,
                                    onStart = onStart,
                                    onInput = onInput,
                                    onCancel = onCancel,
                                )
                            }
                            item {
                                YoutubeSessionStatusCard(
                                    state = state,
                                    onRefresh = onRefresh,
                                    onDisconnect = onDisconnect,
                                )
                            }
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
private fun YoutubeSessionPrimary(
    state: YoutubeSessionState,
    onRefresh: () -> Unit,
    onStart: () -> Unit,
    onInput: (YoutubeRemoteBrowserInput) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item {
            YoutubeSessionPrimaryContent(state, onRefresh, onStart, onInput, onCancel)
        }
    }
}

@Composable
private fun YoutubeSessionPrimaryContent(
    state: YoutubeSessionState,
    onRefresh: () -> Unit,
    onStart: () -> Unit,
    onInput: (YoutubeRemoteBrowserInput) -> Unit,
    onCancel: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.youtube_session_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = stringResource(R.string.youtube_session_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Text(
                text = stringResource(R.string.youtube_session_secondary_account),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        state.errorMessage?.let { message ->
            YoutubeSessionFailure(message, state.errorRequestId, onRefresh)
        }
        if (state.remoteBrowserOpen) {
            YoutubeRemoteBrowserPane(
                state = state,
                onInput = onInput,
                onCancel = onCancel,
            )
        } else {
            YoutubeSessionStart(state = state, onStart = onStart)
        }
    }
}

@Composable
private fun YoutubeSessionStart(state: YoutubeSessionState, onStart: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (state.availability) {
            YoutubeSessionAvailability.Checking -> Text(stringResource(R.string.youtube_session_status_loading))
            YoutubeSessionAvailability.Disabled -> Text(stringResource(R.string.youtube_session_disabled))
            YoutubeSessionAvailability.Unavailable -> {
                Text(stringResource(R.string.youtube_session_unavailable))
                Text(
                    text = stringResource(youtubeRemoteLoginReason(state.unavailableReason)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            YoutubeSessionAvailability.Available -> Unit
        }
        Button(onClick = onStart, enabled = state.canStart) {
            if (state.isStarting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.youtube_session_connect))
            }
        }
    }
}

@Composable
private fun YoutubeSessionFailure(message: String, requestId: String?, onRetry: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.errorContainer) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
            requestId?.let { RequestIdRow(requestId = it) }
            TextButton(onClick = onRetry, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.state_retry))
            }
        }
    }
}

@Composable
private fun YoutubeSessionNotice.message(): String = stringResource(
    when (this) {
        YoutubeSessionNotice.Connected -> R.string.youtube_session_connected_notice
        YoutubeSessionNotice.Disconnected -> R.string.youtube_session_disconnected_notice
        YoutubeSessionNotice.SignInCancelled -> R.string.youtube_session_cancelled_notice
    },
)

private val TABLET_BREAKPOINT = 720.dp

internal fun youtubeRemoteLoginReason(reason: String?): Int = when (reason) {
    "not_configured" -> R.string.youtube_session_reason_not_configured
    "token_unreachable" -> R.string.youtube_session_reason_token_unreachable
    else -> R.string.youtube_session_reason_unknown
}
