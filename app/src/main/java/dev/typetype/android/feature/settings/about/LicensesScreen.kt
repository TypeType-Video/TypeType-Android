package dev.typetype.android.feature.settings.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.variant.LibraryDetailMode
import dev.typetype.android.R
import dev.typetype.android.feature.settings.SettingsDetailTopBar

@Composable
fun LicensesScreen(
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val libraries = remember { buildLibraries(context) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsDetailTopBar(
                title = stringResource(R.string.settings_licenses_title),
                onNavigateBack = onNavigateBack,
            )
            LibrariesContainer(
                libraries = libraries,
                modifier = Modifier.fillMaxSize(),
                detailMode = LibraryDetailMode.Sheet,
            )
        }
    }
}
