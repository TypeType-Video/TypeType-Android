package dev.typetype.android

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.typetype.android.core.ui.navigation.AboutRoute
import dev.typetype.android.core.ui.navigation.AccountsRoute
import dev.typetype.android.core.ui.navigation.AppearanceRoute
import dev.typetype.android.core.ui.navigation.BlockedSettingsRoute
import dev.typetype.android.core.ui.navigation.ContentSettingsRoute
import dev.typetype.android.core.ui.navigation.DiagnosticsRoute
import dev.typetype.android.core.ui.navigation.ImportDataRoute
import dev.typetype.android.core.ui.navigation.LicensesRoute
import dev.typetype.android.core.ui.navigation.PlayerSettingsRoute
import dev.typetype.android.core.ui.navigation.PrivacySettingsRoute
import dev.typetype.android.core.ui.navigation.ProfileSettingsRoute
import dev.typetype.android.core.ui.navigation.RssFeedsRoute
import dev.typetype.android.core.ui.navigation.SettingsRoute
import dev.typetype.android.core.ui.navigation.StorageSettingsRoute
import dev.typetype.android.feature.settings.SettingsRoute as SettingsMenu

internal val LocalSettingsTwoPane = staticCompositionLocalOf { false }

@Composable
internal fun AdaptiveSettingsHost(
    navController: NavHostController,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val entry by navController.currentBackStackEntryAsState()
    val selectedTitle = entry?.destination?.settingsTitle()
    BoxWithConstraints(modifier.fillMaxSize()) {
        val twoPane = maxWidth >= 840.dp && maxHeight >= 480.dp && selectedTitle != null
        CompositionLocalProvider(LocalSettingsTwoPane provides twoPane) {
            Row(Modifier.fillMaxSize()) {
                if (twoPane) {
                    Box(Modifier.width(300.dp)) {
                        SettingsMenu(
                            selectedTitleRes = selectedTitle,
                            onNavigateBack = {
                                if (entry?.destination?.hasRoute<SettingsRoute>() == true) {
                                    navController.popBackStack()
                                } else if (navController.popBackStack<SettingsRoute>(false)) {
                                    navController.popBackStack()
                                } else {
                                    navController.popBackStack()
                                }
                            },
                            onOpenAccounts = { navController.selectSettings(AccountsRoute) },
                            onOpenProfile = { navController.selectSettings(ProfileSettingsRoute) },
                            onOpenImport = { navController.selectSettings(ImportDataRoute) },
                            onOpenRssFeeds = { navController.selectSettings(RssFeedsRoute) },
                            onOpenAppearance = { navController.selectSettings(AppearanceRoute) },
                            onOpenContent = { navController.selectSettings(ContentSettingsRoute) },
                            onOpenPlayer = { navController.selectSettings(PlayerSettingsRoute) },
                            onOpenStorage = { navController.selectSettings(StorageSettingsRoute) },
                            onOpenPrivacy = { navController.selectSettings(PrivacySettingsRoute) },
                            onOpenDiagnostics = { navController.selectSettings(DiagnosticsRoute) },
                            onOpenBlocked = { navController.selectSettings(BlockedSettingsRoute) },
                            onOpenAbout = { navController.selectSettings(AboutRoute) },
                            onSignOut = onSignOut,
                        )
                    }
                    VerticalDivider()
                }
                Box(Modifier.weight(1f).fillMaxSize()) { content() }
            }
        }
    }
}

internal fun NavHostController.selectSettings(route: Any) {
    val current = currentDestination ?: return
    if (current.hasRoute(route::class)) return
    navigate(route) {
        popUpTo(current.id) {
            inclusive = !current.hasRoute<SettingsRoute>()
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavDestination.settingsTitle(): Int? = when {
    hasRoute<SettingsRoute>() || hasRoute<AppearanceRoute>() -> R.string.settings_appearance_title
    hasRoute<AccountsRoute>() -> R.string.accounts_title
    hasRoute<ProfileSettingsRoute>() -> R.string.settings_profile_title
    hasRoute<ImportDataRoute>() -> R.string.settings_import_title
    hasRoute<RssFeedsRoute>() -> R.string.rss_settings_title
    hasRoute<ContentSettingsRoute>() -> R.string.settings_content_title
    hasRoute<PlayerSettingsRoute>() -> R.string.settings_player_title
    hasRoute<StorageSettingsRoute>() -> R.string.settings_storage_title
    hasRoute<PrivacySettingsRoute>() -> R.string.settings_privacy_title
    hasRoute<DiagnosticsRoute>() -> R.string.diagnostics_title
    hasRoute<BlockedSettingsRoute>() -> R.string.settings_blocked_title
    hasRoute<AboutRoute>() || hasRoute<LicensesRoute>() -> R.string.settings_about_title
    else -> null
}
