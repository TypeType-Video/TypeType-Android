package dev.typetype.android.feature.settings.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R

private val QUALITY_OPTIONS = listOf("144p", "240p", "360p", "480p", "720p", "1080p", "1440p", "2160p")
private val AUTOPLAY_COUNTDOWN_OPTIONS = listOf(0, 5, 10, 15, 30, 60)

private data class ServiceOption(
    val id: Int,
    val labelRes: Int,
    val iconRes: Int,
    val brandColor: androidx.compose.ui.graphics.Color,
)

private val SERVICES = listOf(
    ServiceOption(0, R.string.settings_default_service_youtube, R.drawable.ic_service_youtube, androidx.compose.ui.graphics.Color(0xFFFF0000)),
    ServiceOption(6, R.string.settings_default_service_niconico, R.drawable.ic_service_niconico, androidx.compose.ui.graphics.Color(0xFF231815)),
    ServiceOption(5, R.string.settings_default_service_bilibili, R.drawable.ic_service_bilibili, androidx.compose.ui.graphics.Color(0xFF00A1D6)),
)

private data class LanguageOption(val code: String, val display: String)

private val LANGUAGE_OPTIONS = listOf(
    LanguageOption("", "Auto"),
    LanguageOption("en", "English"),
    LanguageOption("fr", "Français"),
    LanguageOption("es", "Español"),
    LanguageOption("de", "Deutsch"),
    LanguageOption("it", "Italiano"),
    LanguageOption("pt", "Português"),
    LanguageOption("ru", "Русский"),
    LanguageOption("ja", "日本語"),
    LanguageOption("ko", "한국어"),
    LanguageOption("zh", "中文"),
    LanguageOption("ar", "العربية"),
    LanguageOption("hi", "हिन्दी"),
    LanguageOption("nl", "Nederlands"),
    LanguageOption("pl", "Polski"),
    LanguageOption("sv", "Svenska"),
    LanguageOption("tr", "Türkçe"),
)

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
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
            TopBar(onNavigateBack = onNavigateBack)
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
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
                    DropdownRow(
                        title = stringResource(R.string.settings_player_autoplay_countdown),
                        subtitle = stringResource(
                            R.string.settings_player_autoplay_countdown_subtitle,
                        ),
                        options = AUTOPLAY_COUNTDOWN_OPTIONS.map {
                            it.toString() to if (it == 0) {
                                stringResource(R.string.settings_player_autoplay_immediate)
                            } else {
                                pluralStringResource(
                                    R.plurals.settings_player_autoplay_seconds,
                                    it,
                                    it,
                                )
                            }
                        },
                        selectedKey = state.autoplayCountdownSeconds.toString(),
                        onSelect = {
                            onAction(PlayerSettingsAction.SetAutoplayCountdown(it.toInt()))
                        },
                        enabled = state.autoplayEnabled,
                    )
                }
                item {
                    SwitchRow(
                        title = stringResource(
                            R.string.settings_player_skip_playlist_autoplay_screen,
                        ),
                        subtitle = stringResource(
                            R.string.settings_player_skip_playlist_autoplay_screen_subtitle,
                        ),
                        checked = state.skipPlaylistAutoplayScreen,
                        onCheckedChange = {
                            onAction(PlayerSettingsAction.SetSkipPlaylistAutoplayScreen(it))
                        },
                    )
                }
                item {
                    DropdownRow(
                        title = stringResource(R.string.settings_player_default_quality),
                        subtitle = stringResource(R.string.settings_player_default_quality_subtitle),
                        options = QUALITY_OPTIONS.map { it to it },
                        selectedKey = state.defaultQuality,
                        onSelect = { onAction(PlayerSettingsAction.SetDefaultQuality(it)) },
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

                if (state.defaultService == 0) {
                    item { Spacer(Modifier.size(4.dp)) }
                    item { SectionHeader(stringResource(R.string.settings_player_section_subtitles)) }
                    item {
                        SwitchRow(
                            title = stringResource(R.string.settings_player_subtitles_enabled),
                            subtitle = stringResource(R.string.settings_player_subtitles_enabled_subtitle),
                            checked = state.subtitlesEnabled,
                            onCheckedChange = { onAction(PlayerSettingsAction.SetSubtitlesEnabled(it)) },
                        )
                    }
                    item {
                        DropdownRow(
                            title = stringResource(R.string.settings_player_subtitle_language),
                            subtitle = stringResource(R.string.settings_player_subtitle_language_subtitle),
                            options = LANGUAGE_OPTIONS.map { it.code to it.display },
                            selectedKey = state.defaultSubtitleLanguage,
                            onSelect = { onAction(PlayerSettingsAction.SetSubtitleLanguage(it)) },
                            enabled = state.subtitlesEnabled,
                        )
                    }
                    item {
                        DropdownRow(
                            title = stringResource(R.string.settings_player_audio_language),
                            subtitle = if (state.preferOriginalLanguage) {
                                stringResource(R.string.settings_player_prefer_original_active)
                            } else {
                                stringResource(R.string.settings_player_audio_language_subtitle)
                            },
                            options = LANGUAGE_OPTIONS.map { it.code to it.display },
                            selectedKey = state.defaultAudioLanguage,
                            onSelect = { onAction(PlayerSettingsAction.SetAudioLanguage(it)) },
                            enabled = !state.preferOriginalLanguage,
                        )
                    }
                    item {
                        SwitchRow(
                            title = stringResource(R.string.settings_player_prefer_original),
                            subtitle = stringResource(R.string.settings_player_prefer_original_subtitle),
                            checked = state.preferOriginalLanguage,
                            onCheckedChange = { onAction(PlayerSettingsAction.SetPreferOriginalLanguage(it)) },
                        )
                    }
                }

                item { Spacer(Modifier.size(4.dp)) }
                item { SectionHeader(stringResource(R.string.settings_section_default_service)) }
                items(SERVICES.size, key = { i -> "svc-${SERVICES[i].id}" }) { i ->
                    val svc = SERVICES[i]
                    ServiceRow(
                        title = stringResource(svc.labelRes),
                        iconRes = svc.iconRes,
                        brandColor = svc.brandColor,
                        selected = state.defaultService == svc.id,
                        onClick = { onAction(PlayerSettingsAction.SetDefaultService(svc.id)) },
                    )
                }

                item { Spacer(Modifier.size(4.dp)) }
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
