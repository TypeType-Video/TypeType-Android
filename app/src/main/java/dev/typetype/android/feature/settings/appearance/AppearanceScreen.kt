package dev.typetype.android.feature.settings.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.SectionHeader
import dev.typetype.android.core.ui.components.KomiStoreAttribution
import dev.typetype.android.domain.preferences.AppearanceMode
import dev.typetype.android.domain.preferences.AppearancePersonality
import dev.typetype.android.domain.preferences.AppPreferences
import dev.typetype.android.feature.settings.SettingsDetailTopBar

@Composable
fun AppearanceRoute(
    onNavigateBack: () -> Unit,
    viewModel: AppearanceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AppearanceScreen(
        state = state,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun AppearanceScreen(
    state: AppPreferences,
    onAction: (AppearanceAction) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item {
                SettingsDetailTopBar(
                    title = stringResource(R.string.settings_appearance_title),
                    onNavigateBack = onNavigateBack,
                )
            }
            item { AppearancePreview(state, Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) }
            appearanceSection(R.string.appearance_style)
            appearancePersonalityItems(state, onAction)
            appearanceSection(R.string.appearance_theme)
            appearanceThemeItems(state, onAction)
            if (state.appearancePersonality == AppearancePersonality.Manga) {
                appearanceSection(R.string.appearance_manga_paper)
                mangaPaperItems(state, onAction)
                appearanceSection(R.string.appearance_manga_details)
                mangaDetailItems(state, onAction)
            }
            appearanceSection(R.string.appearance_mode)
            appearanceModeItems(state, onAction)
            if (state.appearanceMode != AppearanceMode.Light) appearanceAmoledItem(state, onAction)
            appearanceSection(R.string.settings_appearance_accent_color)
            item { AccentChooser(state.accentColor, onAction) }
            appearanceSection(R.string.appearance_typography)
            appearanceFontItems(state, onAction)
            appearanceSection(R.string.appearance_motion)
            appearanceMotionItems(state, onAction)
            item { KomiStoreAttribution() }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.appearanceSection(title: Int) {
    item(key = "section-$title") {
        SectionHeader(
            text = stringResource(title),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}
