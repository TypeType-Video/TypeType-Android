package dev.typetype.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import dev.typetype.android.core.ui.branding.DeArrowBrandingEnvironment
import dev.typetype.android.core.ui.branding.DeArrowBrandingViewModel
import dev.typetype.android.core.ui.components.resolveProfileAvatarUrl
import dev.typetype.android.core.ui.navigation.LoginRoute
import dev.typetype.android.core.ui.navigation.WelcomeRoute
import dev.typetype.android.core.ui.player.PlayerHostController
import dev.typetype.android.domain.branding.DeArrowPreferences
import dev.typetype.android.domain.preferences.AppearanceMotion
import dev.typetype.android.domain.profile.Profile
import dev.typetype.android.domain.server.Server
import dev.typetype.android.domain.usersettings.UserSettings
import kotlinx.coroutines.flow.collectLatest

internal fun AppearanceMotion.navigationTransitionMillis(): Int = when (this) {
    AppearanceMotion.Full -> 280
    AppearanceMotion.Subtle -> 140
    AppearanceMotion.Off -> 0
}

@Composable
internal fun rememberDeArrowEnvironment(
    viewModel: DeArrowBrandingViewModel,
    settings: UserSettings,
): DeArrowBrandingEnvironment = remember(settings, viewModel) {
    DeArrowBrandingEnvironment(
        enabled = settings.deArrowEnabled,
        preferences = DeArrowPreferences(
            titleMode = settings.deArrowTitleMode,
            thumbnailMode = settings.deArrowThumbnailMode,
            trustMode = settings.deArrowTrustMode,
        ),
        loader = viewModel::load,
    )
}

@Composable
internal fun rememberProfileAvatarUrl(profile: Profile?, server: Server?): String? = remember(profile, server) {
    profile?.let {
        resolveProfileAvatarUrl(
            serverBaseUrl = server?.baseUrl,
            avatarUrl = it.avatarUrl,
            avatarType = it.avatarType,
            avatarCode = it.avatarCode,
            fallbackSeed = "${it.id}:${it.publicUsername}",
        )
    }
}

@Composable
internal fun MainNavigationEffect(
    mainViewModel: MainViewModel,
    navController: NavHostController,
    playerHostController: PlayerHostController,
) {
    LaunchedEffect(mainViewModel) {
        mainViewModel.events.collectLatest { event ->
            playerHostController.hide()
            when (event) {
                MainEvent.NavigateToWelcome -> navController.navigate(WelcomeRoute) {
                    popUpTo(0) { inclusive = true }
                }
                is MainEvent.NavigateToLogin -> navController.navigate(LoginRoute(serverId = event.serverId)) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }
}
