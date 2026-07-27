package dev.typetype.android.feature.player.sleep

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.domain.playback.PlaybackSleepTimerMode
import dev.typetype.android.domain.playback.PlaybackSleepTimerState

@Composable
internal fun PlaybackSleepTimerControls(
    viewModel: PlaybackSleepTimerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheetVisible by remember { mutableStateOf(false) }
    SleepTimerSummary(state = state, onClick = { sheetVisible = true })
    if (sheetVisible) {
        SleepTimerSheet(
            state = state,
            onStart = viewModel::start,
            onEndOfVideo = viewModel::stopAtEndOfVideo,
            onCancel = viewModel::cancel,
            onDismiss = { sheetVisible = false },
        )
    }
}

@Composable
private fun SleepTimerSummary(
    state: PlaybackSleepTimerState,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Bedtime,
            contentDescription = null,
            tint = if (state.isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.sleep_timer_title),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = state.summary(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = stringResource(R.string.sleep_timer_open),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerSheet(
    state: PlaybackSleepTimerState,
    onStart: (Int) -> Unit,
    onEndOfVideo: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.sleep_timer_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            TIMER_MINUTES.forEach { minutes ->
                SleepTimerOption(
                    label = pluralStringResource(R.plurals.sleep_timer_minutes, minutes, minutes),
                    selected = state.mode == PlaybackSleepTimerMode.Timed &&
                        state.durationMillis == minutes * 60_000L,
                    onClick = {
                        onStart(minutes)
                        onDismiss()
                    },
                )
            }
            SleepTimerOption(
                label = stringResource(R.string.sleep_timer_end_of_video),
                selected = state.mode == PlaybackSleepTimerMode.EndOfVideo,
                onClick = {
                    onEndOfVideo()
                    onDismiss()
                },
            )
            if (state.isActive) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onCancel()
                            onDismiss()
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.TimerOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.sleep_timer_cancel),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun SleepTimerOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 14.dp),
        )
    }
}

@Composable
internal fun PlaybackSleepTimerState.summary(): String = when (mode) {
    PlaybackSleepTimerMode.Off -> stringResource(R.string.sleep_timer_off)
    PlaybackSleepTimerMode.EndOfVideo -> stringResource(R.string.sleep_timer_end_of_video)
    PlaybackSleepTimerMode.Timed -> stringResource(
        R.string.sleep_timer_remaining,
        formatRemainingTime(remainingMillis),
    )
}

private fun formatRemainingTime(remainingMillis: Long): String {
    val seconds = (remainingMillis.coerceAtLeast(0L) + 999L) / 1_000L
    val hours = seconds / 3_600L
    val minutes = seconds % 3_600L / 60L
    val remainingSeconds = seconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, remainingSeconds)
    } else {
        "%d:%02d".format(minutes, remainingSeconds)
    }
}

private val TIMER_MINUTES = listOf(15, 30, 45, 60, 90)
