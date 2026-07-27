package dev.typetype.android.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.PlaybackException
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.RequestIdRow
import dev.typetype.android.feature.player.error.PlaybackFailureKind
import dev.typetype.android.feature.player.error.classifyPlaybackError
import dev.typetype.android.feature.player.error.playbackRequestId

@Composable
internal fun PlaybackFailureOverlay(
    error: PlaybackException,
    onRetry: () -> Unit,
    onOpenAccounts: () -> Unit,
    onBack: () -> Unit,
) {
    val failure = classifyPlaybackError(error)
    val message = stringResource(failure.messageResource())
    val requestId = playbackRequestId(error)
    val requiresAuthentication = failure == PlaybackFailureKind.AuthenticationExpired
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f))
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.state_couldnt_load_video),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
            )
            requestId?.let { RequestIdRow(requestId = it) }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.state_go_back))
                }
                Button(onClick = if (requiresAuthentication) onOpenAccounts else onRetry) {
                    Text(
                        stringResource(
                            if (requiresAuthentication) {
                                R.string.state_open_accounts
                            } else {
                                R.string.state_retry
                            },
                        ),
                    )
                }
            }
        }
    }
}

private fun PlaybackFailureKind.messageResource(): Int = when (this) {
    PlaybackFailureKind.AuthenticationExpired -> R.string.state_authentication_expired_message
    PlaybackFailureKind.PlaybackSessionExpired -> R.string.state_playback_session_expired
    PlaybackFailureKind.PlaybackGenerationChanged -> R.string.state_playback_generation_changed
    PlaybackFailureKind.YouTubeSessionRequired -> R.string.state_youtube_session_required_message
    PlaybackFailureKind.SabrServerContract -> R.string.state_sabr_server_contract_failed
    PlaybackFailureKind.SabrRecoveryExhausted -> R.string.state_sabr_recovery_exhausted
    PlaybackFailureKind.MediaDelivery -> R.string.state_media_delivery_failed
    PlaybackFailureKind.Network -> R.string.state_playback_network_failed
    PlaybackFailureKind.UnsupportedFormat -> R.string.state_playback_format_unsupported
    PlaybackFailureKind.CleartextBlocked -> R.string.state_playback_cleartext_blocked
    PlaybackFailureKind.BehindLiveWindow -> R.string.state_playback_live_window_moved
    PlaybackFailureKind.Generic -> R.string.state_failed_to_load_stream
}
