package dev.typetype.android.feature.settings.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R

@Composable
fun PlayerSettingsRoute(
    onNavigateBack: () -> Unit,
    viewModel: PlayerSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PlayerSettingsScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onAction = viewModel::onAction,
    )
}

@Composable
fun PlayerSettingsScreen(
    state: PlayerSettingsState,
    onNavigateBack: () -> Unit,
    onAction: (PlayerSettingsAction) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars),
        ) {
            TopBar(onNavigateBack = onNavigateBack)
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { SectionHeader(stringResource(R.string.settings_player_section_gestures)) }
                item {
                    SwitchRow(
                        title = stringResource(R.string.settings_player_double_tap_seek),
                        subtitle = stringResource(R.string.settings_player_double_tap_seek_subtitle),
                        checked = state.doubleTapSeekEnabled,
                        onCheckedChange = { onAction(PlayerSettingsAction.SetDoubleTapSeek(it)) },
                    )
                }
                item {
                    SwitchRow(
                        title = stringResource(R.string.settings_player_swipe_seek),
                        subtitle = stringResource(R.string.settings_player_swipe_seek_subtitle),
                        checked = state.swipeSeekEnabled,
                        onCheckedChange = { onAction(PlayerSettingsAction.SetSwipeSeek(it)) },
                    )
                }
                item {
                    SwitchRow(
                        title = stringResource(R.string.settings_player_swipe_brightness_volume),
                        subtitle = stringResource(R.string.settings_player_swipe_brightness_volume_subtitle),
                        checked = state.swipeBrightnessVolumeEnabled,
                        onCheckedChange = { onAction(PlayerSettingsAction.SetSwipeBrightnessVolume(it)) },
                    )
                }
                item {
                    SwitchRow(
                        title = stringResource(R.string.settings_player_long_press_speed),
                        subtitle = stringResource(R.string.settings_player_long_press_speed_subtitle),
                        checked = state.longPressSpeedEnabled,
                        onCheckedChange = { onAction(PlayerSettingsAction.SetLongPressSpeed(it)) },
                    )
                }
                item { Spacer(Modifier.width(16.dp)) }
                item { SectionHeader(stringResource(R.string.settings_player_section_playback)) }
                item {
                    SwitchRow(
                        title = stringResource(R.string.settings_player_autoplay),
                        subtitle = null,
                        checked = state.autoplayEnabled,
                        onCheckedChange = { onAction(PlayerSettingsAction.SetAutoplay(it)) },
                    )
                }
                item {
                    SwitchRow(
                        title = stringResource(R.string.settings_player_pause_in_background),
                        subtitle = null,
                        checked = state.pauseInBackgroundEnabled,
                        onCheckedChange = { onAction(PlayerSettingsAction.SetPauseInBackground(it)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.settings_back),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = stringResource(R.string.settings_player_title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
