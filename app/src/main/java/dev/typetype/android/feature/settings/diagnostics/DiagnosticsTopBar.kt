package dev.typetype.android.feature.settings.diagnostics

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.typetype.android.R
import dev.typetype.android.feature.settings.SettingsDetailTopBar

@Composable
internal fun DiagnosticsTopBar(onNavigateBack: () -> Unit) {
    SettingsDetailTopBar(
        title = stringResource(R.string.diagnostics_title),
        onNavigateBack = onNavigateBack,
    )
}
