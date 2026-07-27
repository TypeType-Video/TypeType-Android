package dev.typetype.android.feature.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.StreamErrorState
import dev.typetype.android.feature.player.error.StreamErrorKind
import dev.typetype.android.feature.player.error.StreamErrorClass

@Composable
fun ErrorState(
    classification: StreamErrorClass,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenAccounts: () -> Unit,
) {
    val illustrationRes = when (classification.kind) {
        StreamErrorKind.MemberOnly,
        StreamErrorKind.GeoRestricted,
        StreamErrorKind.PaidContent,
        StreamErrorKind.ScheduledPremiere,
        -> R.raw.member_only
        StreamErrorKind.Generic,
        StreamErrorKind.AuthenticationExpired,
        StreamErrorKind.LiveUnsupported,
        StreamErrorKind.NetworkUnavailable,
        StreamErrorKind.SabrInvalidIndex,
        StreamErrorKind.SabrPreparationFailed,
        StreamErrorKind.SabrPreparationTimedOut,
        StreamErrorKind.SabrUnavailable,
        StreamErrorKind.SubtitleInventoryUnavailable,
        StreamErrorKind.ServerContract,
        StreamErrorKind.YouTubeSessionRequired,
        -> R.raw.error_cat
    }
    val displayMessage = when (classification.kind) {
        StreamErrorKind.MemberOnly -> stringResource(R.string.state_member_only_message)
        StreamErrorKind.PaidContent -> stringResource(R.string.video_paid_message)
        StreamErrorKind.ScheduledPremiere -> stringResource(R.string.video_scheduled_message)
        StreamErrorKind.GeoRestricted ->
            classification.rawMessage ?: stringResource(R.string.state_failed_to_load_stream)
        StreamErrorKind.AuthenticationExpired ->
            stringResource(R.string.state_authentication_expired_message)
        StreamErrorKind.YouTubeSessionRequired ->
            stringResource(R.string.state_youtube_session_required_message)
        StreamErrorKind.SabrUnavailable -> stringResource(R.string.state_sabr_unavailable_message)
        StreamErrorKind.SabrInvalidIndex -> stringResource(R.string.state_sabr_invalid_index)
        StreamErrorKind.SabrPreparationFailed ->
            stringResource(R.string.state_sabr_preparation_failed)
        StreamErrorKind.SabrPreparationTimedOut ->
            stringResource(R.string.state_sabr_preparation_timed_out)
        StreamErrorKind.SubtitleInventoryUnavailable ->
            stringResource(R.string.state_subtitle_inventory_unavailable)
        StreamErrorKind.ServerContract -> stringResource(R.string.state_android_playback_incompatible)
        StreamErrorKind.NetworkUnavailable -> stringResource(R.string.state_stream_network_unavailable)
        StreamErrorKind.LiveUnsupported -> stringResource(R.string.state_live_playback_unsupported)
        StreamErrorKind.Generic -> stringResource(R.string.state_failed_to_load_stream)
    }
    val displayTitle = when (classification.kind) {
        StreamErrorKind.MemberOnly -> stringResource(R.string.video_members_only_title)
        StreamErrorKind.PaidContent -> stringResource(R.string.video_paid_title)
        StreamErrorKind.ScheduledPremiere -> stringResource(R.string.video_scheduled_title)
        else -> stringResource(R.string.state_couldnt_load_video)
    }
    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.player_back),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        StreamErrorState(
            title = displayTitle,
            message = displayMessage,
            illustrationRes = illustrationRes,
            countryCode = classification.countryCode,
            requestId = classification.requestId,
            onRetry = if (classification.kind == StreamErrorKind.AuthenticationExpired) {
                onOpenAccounts
            } else {
                onRetry
            },
            retryLabel = if (classification.kind == StreamErrorKind.AuthenticationExpired) {
                stringResource(R.string.state_open_accounts)
            } else {
                null
            },
            onBack = onNavigateBack,
        )
    }
}
