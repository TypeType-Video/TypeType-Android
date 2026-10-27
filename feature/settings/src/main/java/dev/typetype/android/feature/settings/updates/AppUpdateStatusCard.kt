package dev.typetype.android.feature.settings.updates

import android.text.format.Formatter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.typetype.android.R

@Composable
internal fun AppUpdateStatusCard(
    versionName: String,
    state: AppUpdateUiState,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    val highlighted = state.status.isActionable()
    val tint by animateFloatAsState(if (highlighted) 0.12f else 0f, label = "update-card-tint")
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = lerp(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.primary, tint),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (highlighted) 0.7f else 0.18f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.animateContentSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AppMark()
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.updates_installed_version, versionName),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    StatusLine(state)
                }
            }
            if (state.status == AppUpdateUiStatus.DOWNLOADING) DownloadProgress(state)
            state.release?.takeIf { state.status == AppUpdateUiStatus.AVAILABLE && it.apkSizeBytes > 0 }?.let {
                Text(
                    stringResource(R.string.updates_download_size, Formatter.formatShortFileSize(LocalContext.current, it.apkSizeBytes)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PrimaryAction(state, onCheck, onDownload, onInstall)
        }
    }
}

@Composable
private fun AppMark() {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.ic_launcher_monochrome), contentDescription = null, modifier = Modifier.size(36.dp))
    }
}

@Composable
private fun StatusLine(state: AppUpdateUiState) {
    AnimatedContent(
        targetState = state.statusText(),
        transitionSpec = { fadeIn(tween(220, delayMillis = 90)) togetherWith fadeOut(tween(90)) },
        label = "update-status",
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    ) { text ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(state.status.dotColor()),
            )
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.status == AppUpdateUiStatus.FAILED) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun DownloadProgress(state: AppUpdateUiState) {
    val progress by animateFloatAsState(state.progress, label = "update-progress")
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LinearProgressIndicator(
            progress = { progress },
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
            modifier = Modifier.fillMaxWidth().height(6.dp),
        )
        if (state.totalBytes > 0) {
            Text(
                stringResource(
                    R.string.updates_progress,
                    Formatter.formatShortFileSize(context, state.downloadedBytes),
                    Formatter.formatShortFileSize(context, state.totalBytes),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PrimaryAction(
    state: AppUpdateUiState,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    val wide = Modifier.fillMaxWidth().height(48.dp)
    when (state.status) {
        AppUpdateUiStatus.AVAILABLE -> Button(onClick = onDownload, modifier = wide) {
            Text(stringResource(R.string.updates_download))
        }
        AppUpdateUiStatus.READY_TO_INSTALL,
        AppUpdateUiStatus.PERMISSION_REQUESTED,
        AppUpdateUiStatus.INSTALLER_OPENED,
        -> Button(onClick = onInstall, modifier = wide) { Text(stringResource(R.string.updates_install)) }
        AppUpdateUiStatus.FAILED -> FilledTonalButton(
            onClick = if (state.release != null) onDownload else onCheck,
            modifier = wide,
        ) { Text(stringResource(R.string.updates_retry)) }
        AppUpdateUiStatus.IDLE,
        AppUpdateUiStatus.CHECKING,
        AppUpdateUiStatus.UP_TO_DATE,
        AppUpdateUiStatus.NEWER_BUILD_INSTALLED,
        -> FilledTonalButton(
            onClick = onCheck,
            enabled = state.status != AppUpdateUiStatus.CHECKING,
            modifier = wide,
        ) { Text(stringResource(R.string.updates_check)) }
        AppUpdateUiStatus.DOWNLOADING,
        AppUpdateUiStatus.MANAGED_BY_F_DROID,
        -> Unit
    }
}

private fun AppUpdateUiStatus.isActionable(): Boolean = when (this) {
    AppUpdateUiStatus.AVAILABLE,
    AppUpdateUiStatus.DOWNLOADING,
    AppUpdateUiStatus.READY_TO_INSTALL,
    AppUpdateUiStatus.PERMISSION_REQUESTED,
    AppUpdateUiStatus.INSTALLER_OPENED,
    -> true
    else -> false
}

@Composable
private fun AppUpdateUiStatus.dotColor() = when (this) {
    AppUpdateUiStatus.FAILED -> MaterialTheme.colorScheme.error
    AppUpdateUiStatus.UP_TO_DATE,
    AppUpdateUiStatus.NEWER_BUILD_INSTALLED,
    AppUpdateUiStatus.MANAGED_BY_F_DROID,
    -> MaterialTheme.colorScheme.tertiary
    AppUpdateUiStatus.IDLE,
    AppUpdateUiStatus.CHECKING,
    -> MaterialTheme.colorScheme.outline
    else -> MaterialTheme.colorScheme.primary
}
