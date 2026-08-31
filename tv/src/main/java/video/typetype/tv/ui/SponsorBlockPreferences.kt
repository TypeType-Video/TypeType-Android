package video.typetype.tv.ui

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import video.typetype.sdk.core.SponsorBlockMode
import video.typetype.sdk.core.UserSettings
import video.typetype.tv.player.SPONSOR_BLOCK_CATEGORIES
import video.typetype.tv.player.SponsorBlockCategory

internal fun LazyListScope.sponsorBlockPreferenceItems(
    settings: UserSettings,
    initialFocus: FocusRequester,
    rightExit: FocusRequester,
    onChange: (UserSettings) -> Unit,
) {
    item {
        SettingsSectionTitle(
            "SponsorBlock behavior",
            "Choose the global behavior, then adjust individual categories.",
        )
    }
    item {
        OptionRow(MODE_LABELS, settings.sponsorBlockMode.label(), rightExit, initialFocus) { label ->
            val mode = label.toSponsorBlockMode()
            onChange(
                settings.copy(
                    sponsorBlockMode = mode,
                    sponsorBlockCategoryActions = SPONSOR_BLOCK_CATEGORIES.associate { it.id to mode },
                ),
            )
        }
    }
    item { SettingsSectionTitle("Categories") }
    SPONSOR_BLOCK_CATEGORIES.forEach { category ->
        item {
            val selected = settings.sponsorBlockCategoryActions[category.id]
                ?: category.defaultMode
            SponsorBlockCategoryRow(category, selected, rightExit) { mode ->
                onChange(
                    settings.copy(
                        sponsorBlockCategoryActions = settings.sponsorBlockCategoryActions +
                            (category.id to mode),
                    ),
                )
            }
        }
    }
    item { SettingsSectionTitle("Minimum segment duration") }
    item {
        val durations = listOf(0, 1, 3, 5, 10)
        OptionRow(durations.map(Int::durationLabel), settings.sponsorBlockMinimumDuration.durationLabel(), rightExit) {
            val seconds = it.substringBefore(' ').toIntOrNull() ?: 0
            onChange(settings.copy(sponsorBlockMinimumDuration = seconds))
        }
    }
    item { SettingsSectionTitle("Player presentation") }
    preference(
        "Current segment",
        "Show the active SponsorBlock category during playback.",
        settings.sponsorBlockShowCurrentSegment,
        rightExit,
    ) { onChange(settings.copy(sponsorBlockShowCurrentSegment = it)) }
    preference(
        "Timeline markers",
        "Mark visible SponsorBlock segments on the playback timeline.",
        settings.sponsorBlockShowChapters,
        rightExit,
    ) { onChange(settings.copy(sponsorBlockShowChapters = it)) }
    preference(
        "Full-video labels",
        "Show categories that cover almost the entire video.",
        settings.sponsorBlockShowFullVideoLabels,
        rightExit,
    ) { onChange(settings.copy(sponsorBlockShowFullVideoLabels = it)) }
    preference(
        "Manual full-video skip",
        "Never automatically skip a category covering the whole video.",
        settings.sponsorBlockManualSkipOnFullVideo,
        rightExit,
    ) { onChange(settings.copy(sponsorBlockManualSkipOnFullVideo = it)) }
    preference(
        "Music-only rule",
        "Skip non-music sections only when the video category is Music.",
        settings.sponsorBlockSkipNonMusicOnlyOnMusicVideos,
        rightExit,
    ) { onChange(settings.copy(sponsorBlockSkipNonMusicOnlyOnMusicVideos = it)) }
    preference(
        "Mute instead of skipping",
        "Silence matching segments while keeping playback continuous.",
        settings.sponsorBlockMuteInsteadOfSkip,
        rightExit,
    ) { onChange(settings.copy(sponsorBlockMuteInsteadOfSkip = it)) }
}

@Composable
private fun SponsorBlockCategoryRow(
    category: SponsorBlockCategory,
    selected: SponsorBlockMode,
    rightExit: FocusRequester,
    onSelect: (SponsorBlockMode) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        colors = androidx.tv.material3.SurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .78f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(11.dp).background(sponsorCategoryColor(category.id), CircleShape),
                )
                Column {
                    Text(category.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        category.id.replace('_', ' '),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MODE_VALUES.forEachIndexed { index, mode ->
                    val active = mode == selected
                    Surface(
                        modifier = Modifier.then(
                            if (index == MODE_VALUES.lastIndex) {
                                Modifier.focusProperties { right = rightExit }
                            } else Modifier,
                        ),
                        onClick = { onSelect(mode) },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (active) {
                                MaterialTheme.colorScheme.primary
                            } else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (active) {
                                MaterialTheme.colorScheme.onPrimary
                            } else MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.primary,
                            focusedContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(mode.label(), modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                }
            }
        }
    }
}

private fun LazyListScope.preference(
    title: String,
    description: String,
    checked: Boolean,
    rightExit: FocusRequester,
    onChange: (Boolean) -> Unit,
) {
    item { ToggleRow(title, description, checked, rightExit, onChange = onChange) }
}

private fun SponsorBlockMode.label(): String = when (this) {
    SponsorBlockMode.AutoSkip -> "Skip"
    SponsorBlockMode.MarkOnly -> "Mark"
    SponsorBlockMode.Disabled -> "Hide"
}

private fun String.toSponsorBlockMode(): SponsorBlockMode = when (this) {
    "Skip" -> SponsorBlockMode.AutoSkip
    "Mark" -> SponsorBlockMode.MarkOnly
    else -> SponsorBlockMode.Disabled
}

private fun Int.durationLabel(): String = if (this <= 0) "0 seconds" else "$this seconds"

private val MODE_LABELS: List<String> = listOf("Skip", "Mark", "Hide")
private val MODE_VALUES: List<SponsorBlockMode> = listOf(
    SponsorBlockMode.AutoSkip,
    SponsorBlockMode.MarkOnly,
    SponsorBlockMode.Disabled,
)
