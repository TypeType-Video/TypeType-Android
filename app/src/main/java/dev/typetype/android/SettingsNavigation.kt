package dev.typetype.android

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import dev.typetype.android.core.ui.navigation.AboutRoute
import dev.typetype.android.core.ui.navigation.AccountsRoute
import dev.typetype.android.core.ui.navigation.AppearanceRoute
import dev.typetype.android.core.ui.navigation.BlockedSettingsRoute
import dev.typetype.android.core.ui.navigation.DiagnosticsRoute
import dev.typetype.android.core.ui.navigation.ImportDataRoute
import dev.typetype.android.core.ui.navigation.PlayerSettingsRoute
import dev.typetype.android.core.ui.navigation.PrivacySettingsRoute
import dev.typetype.android.core.ui.navigation.ProfileSettingsRoute
import dev.typetype.android.core.ui.navigation.SettingsRoute
import dev.typetype.android.core.ui.navigation.StorageSettingsRoute
import dev.typetype.android.core.ui.navigation.YoutubeSessionRoute
import dev.typetype.android.feature.settings.SettingsScreen
import dev.typetype.android.feature.settings.imports.ImportDataRoute as ImportDataRouteScreen
import dev.typetype.android.feature.settings.youtubesession.YoutubeSessionRoute as YoutubeSessionRouteScreen

internal fun NavGraphBuilder.settingsDestinations(
    navController: NavHostController,
    importsAvailable: Boolean,
    youtubeSessionAvailable: Boolean,
    onSignOut: () -> Unit,
) {
    composable<SettingsRoute> {
        SettingsScreen(
            onNavigateBack = { navController.popBackStack() },
            onOpenAccounts = { navController.navigate(AccountsRoute) },
            onOpenProfile = { navController.navigate(ProfileSettingsRoute) },
            onOpenImport = { navController.navigate(ImportDataRoute) },
            importsAvailable = importsAvailable,
            onOpenYoutubeSession = { navController.navigate(YoutubeSessionRoute) },
            youtubeSessionAvailable = youtubeSessionAvailable,
            onOpenAppearance = { navController.navigate(AppearanceRoute) },
            onOpenPlayer = { navController.navigate(PlayerSettingsRoute) },
            onOpenStorage = { navController.navigate(StorageSettingsRoute) },
            onOpenPrivacy = { navController.navigate(PrivacySettingsRoute) },
            onOpenDiagnostics = { navController.navigate(DiagnosticsRoute) },
            onOpenBlocked = { navController.navigate(BlockedSettingsRoute) },
            onOpenAbout = { navController.navigate(AboutRoute) },
            onSignOut = onSignOut,
        )
    }
    composable<ImportDataRoute> {
        ImportDataRouteScreen(
            onNavigateBack = { navController.popBackStack() },
        )
    }
    composable<YoutubeSessionRoute> {
        YoutubeSessionRouteScreen(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}
