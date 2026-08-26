package dev.typetype.android.feature.settings.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.res.stringResource
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.DropdownRow
import dev.typetype.android.core.ui.components.SettingsSectionHeader
import dev.typetype.android.core.ui.components.SwitchRow
import dev.typetype.android.domain.usersettings.SponsorBlockMode

private data class SponsorBlockCategorySetting(
    val key: String,
    val labelResource: Int,
)

private val SPONSOR_BLOCK_CATEGORIES = listOf(
    SponsorBlockCategorySetting("sponsor", R.string.player_sponsorblock_category_sponsor),
    SponsorBlockCategorySetting("selfpromo", R.string.player_sponsorblock_category_self_promotion),
    SponsorBlockCategorySetting("exclusive_access", R.string.player_sponsorblock_category_exclusive_access),
    SponsorBlockCategorySetting("interaction", R.string.player_sponsorblock_category_interaction),
    SponsorBlockCategorySetting("poi_highlight", R.string.player_sponsorblock_category_highlight),
    SponsorBlockCategorySetting("intro", R.string.player_sponsorblock_category_intro),
    SponsorBlockCategorySetting("outro", R.string.player_sponsorblock_category_outro),
    SponsorBlockCategorySetting("preview", R.string.player_sponsorblock_category_preview),
    SponsorBlockCategorySetting("filler", R.string.player_sponsorblock_category_filler),
    SponsorBlockCategorySetting("chapter", R.string.player_sponsorblock_category_chapter),
    SponsorBlockCategorySetting("music_offtopic", R.string.player_sponsorblock_category_music),
)

internal fun LazyListScope.sponsorBlockSettingsItems(
    state: PlayerSettingsState,
    onAction: (PlayerSettingsAction) -> Unit,
) {
    item { Spacer(Modifier.size(4.dp)) }
    item { SettingsSectionHeader(stringResource(R.string.settings_player_section_sponsorblock)) }
    item {
        DropdownRow(
            title = stringResource(R.string.settings_player_sponsorblock_mode),
            subtitle = stringResource(R.string.settings_player_sponsorblock_mode_subtitle),
            options = sponsorBlockModeOptions(),
            selectedKey = state.sponsorBlockMode.wireValue,
            onSelect = {
                onAction(PlayerSettingsAction.SetSponsorBlockMode(SponsorBlockMode.fromWireValue(it)))
            },
        )
    }
    if (state.sponsorBlockMode != SponsorBlockMode.Disabled) {
        item {
            SponsorBlockMinimumDurationRow(
                seconds = state.sponsorBlockMinimumDuration,
                onChange = {
                    onAction(PlayerSettingsAction.SetSponsorBlockMinimumDuration(it))
                },
            )
        }
        items(
            SPONSOR_BLOCK_CATEGORIES.size,
            key = { "sponsor-${SPONSOR_BLOCK_CATEGORIES[it].key}" },
            contentType = { "sponsor-category" },
        ) {
            val category = SPONSOR_BLOCK_CATEGORIES[it]
            DropdownRow(
                title = stringResource(category.labelResource),
                subtitle = null,
                options = sponsorBlockModeOptions(),
                selectedKey = state.sponsorBlockCategoryActions[category.key]
                    ?.wireValue
                    ?: SponsorBlockMode.MarkOnly.wireValue,
                onSelect = { value ->
                    onAction(
                        PlayerSettingsAction.SetSponsorBlockCategory(
                            category.key,
                            SponsorBlockMode.fromWireValue(value),
                        ),
                    )
                },
            )
        }
        sponsorBlockToggle(
            R.string.settings_player_sponsorblock_show_current,
            state.sponsorBlockShowCurrentSegment,
            SponsorBlockOption.ShowCurrentSegment,
            onAction,
        )
        sponsorBlockToggle(
            R.string.settings_player_sponsorblock_show_chapters,
            state.sponsorBlockShowChapters,
            SponsorBlockOption.ShowChapters,
            onAction,
        )
        sponsorBlockToggle(
            R.string.settings_player_sponsorblock_show_full_video,
            state.sponsorBlockShowFullVideoLabels,
            SponsorBlockOption.ShowFullVideoLabels,
            onAction,
        )
        sponsorBlockToggle(
            R.string.settings_player_sponsorblock_manual_full_video,
            state.sponsorBlockManualSkipOnFullVideo,
            SponsorBlockOption.ManualSkipOnFullVideo,
            onAction,
        )
        sponsorBlockToggle(
            R.string.settings_player_sponsorblock_music_only,
            state.sponsorBlockSkipNonMusicOnlyOnMusicVideos,
            SponsorBlockOption.SkipNonMusicOnlyOnMusicVideos,
            onAction,
        )
        sponsorBlockToggle(
            R.string.settings_player_sponsorblock_mute,
            state.sponsorBlockMuteInsteadOfSkip,
            SponsorBlockOption.MuteInsteadOfSkip,
            onAction,
        )
    }
}

private fun LazyListScope.sponsorBlockToggle(
    titleResource: Int,
    checked: Boolean,
    option: SponsorBlockOption,
    onAction: (PlayerSettingsAction) -> Unit,
) {
    item {
        SwitchRow(
            title = stringResource(titleResource),
            subtitle = null,
            checked = checked,
            onCheckedChange = {
                onAction(PlayerSettingsAction.SetSponsorBlockOption(option, it))
            },
        )
    }
}

@Composable
private fun SponsorBlockMinimumDurationRow(
    seconds: Int,
    onChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_player_sponsorblock_minimum_duration),
            style = MaterialTheme.typography.bodyLarge,
        )
        OutlinedTextField(
            value = seconds.toString(),
            onValueChange = { value -> value.toIntOrNull()?.let(onChange) },
            suffix = { Text(stringResource(R.string.settings_player_seconds_short)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
        )
    }
}

@Composable
private fun sponsorBlockModeOptions(): List<Pair<String, String>> = listOf(
    SponsorBlockMode.AutoSkip.wireValue to stringResource(R.string.settings_player_sponsorblock_skip),
    SponsorBlockMode.MarkOnly.wireValue to stringResource(R.string.settings_player_sponsorblock_mark),
    SponsorBlockMode.Disabled.wireValue to stringResource(R.string.settings_player_sponsorblock_hide),
)
