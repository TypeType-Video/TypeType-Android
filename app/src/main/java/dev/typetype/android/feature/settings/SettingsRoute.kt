package dev.typetype.android.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsRoute(
    onNavigateBack: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenYoutubeSession: () -> Unit,
    onOpenRssFeeds: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenContent: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenBlocked: () -> Unit,
    onOpenAbout: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(
        onNavigateBack = onNavigateBack,
        onOpenAccounts = onOpenAccounts,
        onOpenProfile = onOpenProfile,
        onOpenImport = onOpenImport,
        importsAvailable = state.accountFeaturesAvailable,
        onOpenYoutubeSession = onOpenYoutubeSession,
        youtubeSessionAvailable = state.accountFeaturesAvailable && state.youtubeSessionAvailable,
        onOpenRssFeeds = onOpenRssFeeds,
        rssAvailable = state.accountFeaturesAvailable && state.rssAvailable,
        onOpenAppearance = onOpenAppearance,
        onOpenContent = onOpenContent,
        onOpenPlayer = onOpenPlayer,
        onOpenStorage = onOpenStorage,
        onOpenPrivacy = onOpenPrivacy,
        onOpenDiagnostics = onOpenDiagnostics,
        onOpenBlocked = onOpenBlocked,
        onOpenAbout = onOpenAbout,
        onSignOut = onSignOut,
    )
}
