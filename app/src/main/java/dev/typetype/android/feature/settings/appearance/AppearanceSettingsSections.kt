package dev.typetype.android.feature.settings.appearance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.TypeTypeSwitch
import dev.typetype.android.domain.preferences.AccentColor
import dev.typetype.android.domain.preferences.AppearanceFont
import dev.typetype.android.domain.preferences.AppearanceMode
import dev.typetype.android.domain.preferences.AppearanceMotion
import dev.typetype.android.domain.preferences.AppearancePersonality
import dev.typetype.android.domain.preferences.AppearanceTheme
import dev.typetype.android.domain.preferences.AppPreferences
import dev.typetype.android.domain.preferences.MangaHeadlineMarker
import dev.typetype.android.domain.preferences.MangaPaper

private data class LabelledOption<T>(val value: T, val title: Int, val subtitle: Int? = null)
private data class AccentSwatch(val accent: AccentColor, val label: Int, val color: Color)

private val personalityOptions = listOf(
    LabelledOption(AppearancePersonality.Classic, R.string.appearance_classic, R.string.appearance_classic_description),
    LabelledOption(AppearancePersonality.Manga, R.string.appearance_manga, R.string.appearance_manga_description),
)
private val modeOptions = listOf(
    LabelledOption(AppearanceMode.System, R.string.appearance_system),
    LabelledOption(AppearanceMode.Light, R.string.appearance_light),
    LabelledOption(AppearanceMode.Dark, R.string.appearance_dark),
)
private val themeOptions = listOf(
    LabelledOption(AppearanceTheme.TypeType, R.string.appearance_theme_typetype, R.string.appearance_theme_typetype_description),
    LabelledOption(AppearanceTheme.Dynamic, R.string.appearance_theme_dynamic, R.string.appearance_theme_dynamic_description),
    LabelledOption(AppearanceTheme.Nord, R.string.appearance_theme_nord),
    LabelledOption(AppearanceTheme.Cream, R.string.appearance_theme_cream),
    LabelledOption(AppearanceTheme.Forest, R.string.appearance_theme_forest),
    LabelledOption(AppearanceTheme.Plum, R.string.appearance_theme_plum),
)
private val paperOptions = listOf(
    LabelledOption(MangaPaper.Day, R.string.appearance_paper_day),
    LabelledOption(MangaPaper.Night, R.string.appearance_paper_night),
    LabelledOption(MangaPaper.Nord, R.string.appearance_paper_nord),
)
private val fontOptions = listOf(
    LabelledOption(AppearanceFont.System, R.string.appearance_font_system, R.string.appearance_font_system_description),
    LabelledOption(AppearanceFont.Expressive, R.string.appearance_font_expressive, R.string.appearance_font_expressive_description),
)
private val motionOptions = listOf(
    LabelledOption(AppearanceMotion.Full, R.string.appearance_motion_full),
    LabelledOption(AppearanceMotion.Subtle, R.string.appearance_motion_subtle),
    LabelledOption(AppearanceMotion.Off, R.string.appearance_motion_off),
)
private val accents = listOf(
    AccentSwatch(AccentColor.System, R.string.accent_system, Color(0xFF8AB4F8)),
    AccentSwatch(AccentColor.Red, R.string.accent_red, Color(0xFFEF4444)),
    AccentSwatch(AccentColor.Blue, R.string.accent_blue, Color(0xFF60A5FA)),
    AccentSwatch(AccentColor.Yellow, R.string.accent_yellow, Color(0xFFFBBF24)),
    AccentSwatch(AccentColor.Green, R.string.accent_green, Color(0xFF34D399)),
    AccentSwatch(AccentColor.Purple, R.string.accent_purple, Color(0xFFC084FC)),
    AccentSwatch(AccentColor.Violet, R.string.accent_violet, Color(0xFFA78BFA)),
    AccentSwatch(AccentColor.Monochrome, R.string.accent_monochrome, Color(0xFFE4E4E7)),
)

fun androidx.compose.foundation.lazy.LazyListScope.appearancePersonalityItems(
    state: AppPreferences,
    onAction: (AppearanceAction) -> Unit,
) = options(personalityOptions, state.appearancePersonality) {
    onAction(AppearanceAction.SelectPersonality(it))
}

fun androidx.compose.foundation.lazy.LazyListScope.appearanceModeItems(
    state: AppPreferences,
    onAction: (AppearanceAction) -> Unit,
) = options(modeOptions, state.appearanceMode) { onAction(AppearanceAction.SelectMode(it)) }

fun androidx.compose.foundation.lazy.LazyListScope.appearanceThemeItems(
    state: AppPreferences,
    onAction: (AppearanceAction) -> Unit,
) = options(themeOptions, state.appearanceTheme) { onAction(AppearanceAction.SelectTheme(it)) }

fun androidx.compose.foundation.lazy.LazyListScope.mangaPaperItems(
    state: AppPreferences,
    onAction: (AppearanceAction) -> Unit,
) = options(paperOptions, state.mangaPaper) { onAction(AppearanceAction.SelectMangaPaper(it)) }

fun androidx.compose.foundation.lazy.LazyListScope.appearanceFontItems(
    state: AppPreferences,
    onAction: (AppearanceAction) -> Unit,
) = options(fontOptions, state.appearanceFont) { onAction(AppearanceAction.SelectFont(it)) }

fun androidx.compose.foundation.lazy.LazyListScope.appearanceMotionItems(
    state: AppPreferences,
    onAction: (AppearanceAction) -> Unit,
) = options(motionOptions, state.appearanceMotion) { onAction(AppearanceAction.SelectMotion(it)) }

private fun <T> androidx.compose.foundation.lazy.LazyListScope.options(
    entries: List<LabelledOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    items(entries.size, contentType = { "appearance-option" }) { index ->
        val option = entries[index]
        AppearanceOptionRow(option, option.value == selected) { onSelect(option.value) }
    }
}

@Composable
private fun <T> AppearanceOptionRow(option: LabelledOption<T>, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().selectable(
            selected = selected,
            role = Role.RadioButton,
            onClick = onClick,
        ).padding(20.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.weight(1f)) {
            Text(stringResource(option.title), style = MaterialTheme.typography.bodyLarge)
            option.subtitle?.let {
                Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

fun androidx.compose.foundation.lazy.LazyListScope.appearanceAmoledItem(
    state: AppPreferences,
    onAction: (AppearanceAction) -> Unit,
) = item {
    AppearanceToggleRow(
        title = R.string.appearance_amoled,
        subtitle = R.string.appearance_amoled_description,
        checked = state.appearanceAmoled,
        onCheckedChange = { onAction(AppearanceAction.SetAmoled(it)) },
    )
}

fun androidx.compose.foundation.lazy.LazyListScope.mangaDetailItems(
    state: AppPreferences,
    onAction: (AppearanceAction) -> Unit,
) {
    val markers = listOf(
        LabelledOption(MangaHeadlineMarker.None, R.string.appearance_marker_none),
        LabelledOption(MangaHeadlineMarker.Stamp, R.string.appearance_marker_stamp),
        LabelledOption(MangaHeadlineMarker.SpeedLines, R.string.appearance_marker_speed_lines),
    )
    options(markers, state.mangaHeadlineMarker) { onAction(AppearanceAction.SelectHeadlineMarker(it)) }
    mangaToggle(R.string.appearance_screentone, MangaDecoration.Screentone, state.mangaScreentone, onAction)
    mangaToggle(R.string.appearance_speed_lines, MangaDecoration.SpeedLines, state.mangaSpeedLines, onAction)
    mangaToggle(R.string.appearance_starburst, MangaDecoration.Starburst, state.mangaStarburst, onAction)
    mangaToggle(R.string.appearance_inked_icons, MangaDecoration.InkedIcons, state.mangaInkedIcons, onAction)
    mangaToggle(R.string.appearance_panel_tilt, MangaDecoration.PanelTilt, state.mangaPanelTilt, onAction)
}

private fun androidx.compose.foundation.lazy.LazyListScope.mangaToggle(
    title: Int,
    decoration: MangaDecoration,
    checked: Boolean,
    onAction: (AppearanceAction) -> Unit,
) = item {
    AppearanceToggleRow(title, null, checked) {
        onAction(AppearanceAction.SetMangaDecoration(decoration, it))
    }
}

@Composable
private fun AppearanceToggleRow(
    title: Int,
    subtitle: Int?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(20.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            subtitle?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall) }
        }
        TypeTypeSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun AccentChooser(current: AccentColor, onAction: (AppearanceAction) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        accents.forEach { swatch ->
            val label = stringResource(swatch.label)
            Column(
                modifier = Modifier.selectable(
                    selected = swatch.accent == current,
                    role = Role.RadioButton,
                ) { onAction(AppearanceAction.SelectAccent(swatch.accent)) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(48.dp).background(swatch.color, CircleShape)
                        .border(if (swatch.accent == current) 3.dp else 1.dp, MaterialTheme.colorScheme.onBackground, CircleShape),
                )
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun AppearancePreview(state: AppPreferences, modifier: Modifier = Modifier) {
    val manga = state.appearancePersonality == AppearancePersonality.Manga
    Surface(
        modifier = modifier.fillMaxWidth().rotate(if (manga && state.mangaPanelTilt) -1f else 0f),
        border = if (manga) BorderStroke(2.dp, MaterialTheme.colorScheme.outline) else null,
        tonalElevation = if (manga) 0.dp else 2.dp,
    ) {
        Box(Modifier.fillMaxWidth().height(132.dp)) {
            if (manga && state.mangaScreentone) MangaPreviewTexture()
            Column(Modifier.align(Alignment.CenterStart).padding(20.dp)) {
                Text(
                    text = stringResource(if (manga) R.string.appearance_preview_manga else R.string.appearance_preview_classic),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(stringResource(R.string.appearance_preview_body), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun MangaPreviewTexture() {
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f)
    Canvas(Modifier.fillMaxWidth().height(132.dp)) {
        var x = 4.dp.toPx()
        val step = 9.dp.toPx()
        while (x < size.width) {
            var y = 4.dp.toPx()
            while (y < size.height) {
                drawCircle(color, 1.dp.toPx(), Offset(x, y))
                y += step
            }
            x += step
        }
    }
}
