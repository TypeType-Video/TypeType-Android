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
import dev.typetype.android.feature.player.error.classifyStreamError

@Composable
fun ErrorState(
    message: String,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val classification = classifyStreamError(message)
    val illustrationRes = when (classification.kind) {
        StreamErrorKind.MemberOnly,
        StreamErrorKind.GeoRestricted,
        -> R.raw.member_only
        StreamErrorKind.Generic -> R.raw.error_cat
    }
    val displayMessage = when (classification.kind) {
        StreamErrorKind.MemberOnly -> stringResource(R.string.state_member_only_message)
        StreamErrorKind.GeoRestricted,
        StreamErrorKind.Generic ->
            classification.rawMessage ?: stringResource(R.string.state_failed_to_load_stream)
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
            title = stringResource(R.string.state_couldnt_load_video),
            message = displayMessage,
            illustrationRes = illustrationRes,
            countryCode = classification.countryCode,
            onRetry = onRetry,
            onBack = onNavigateBack,
        )
    }
}
