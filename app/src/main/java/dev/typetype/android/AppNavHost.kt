package dev.typetype.android

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.core.ui.branding.DeArrowBrandingEnvironment
import dev.typetype.android.core.ui.branding.DeArrowBrandingViewModel
import dev.typetype.android.core.ui.branding.LocalDeArrowBranding
import dev.typetype.android.core.ui.components.resolveProfileAvatarUrl
import dev.typetype.android.core.ui.navigation.AboutRoute
import dev.typetype.android.core.ui.navigation.AccountsRoute
import dev.typetype.android.core.ui.navigation.AddServerRoute
import dev.typetype.android.core.ui.navigation.BlockedSettingsRoute
import dev.typetype.android.core.ui.navigation.DiagnosticsRoute
import dev.typetype.android.core.ui.navigation.PrivacySettingsRoute
import dev.typetype.android.core.ui.navigation.PublicPlaylistRoute
import dev.typetype.android.core.ui.navigation.ProfileSettingsRoute
import dev.typetype.android.core.ui.share.LocalServerBaseUrl
import dev.typetype.android.core.ui.navigation.AppearanceRoute
import dev.typetype.android.core.ui.navigation.ChannelRoute
import dev.typetype.android.core.ui.navigation.ContentSettingsRoute
import dev.typetype.android.core.ui.navigation.HomeRoute
import dev.typetype.android.core.ui.navigation.LoginRoute
import dev.typetype.android.core.ui.navigation.NotificationsRoute
import dev.typetype.android.core.ui.navigation.PlayerSettingsRoute
import dev.typetype.android.core.ui.navigation.PlaylistRoute
import dev.typetype.android.core.ui.navigation.SearchRoute
import dev.typetype.android.core.ui.navigation.SettingsRoute
import dev.typetype.android.core.ui.navigation.StorageSettingsRoute
import dev.typetype.android.core.ui.navigation.SubscriptionsRoute
import dev.typetype.android.core.ui.navigation.WelcomeRoute
import dev.typetype.android.domain.branding.DeArrowPreferences
import dev.typetype.android.feature.home.HomeRoute as HomeRouteScreen
import dev.typetype.android.feature.home.HomeViewModel
import dev.typetype.android.feature.library.playlist.PlaylistRoute as PlaylistRouteScreen
import dev.typetype.android.feature.notifications.rememberNotificationBadge
import dev.typetype.android.feature.search.SearchRoute as SearchRouteScreen
import dev.typetype.android.feature.settings.accounts.AccountSettingsRoute as AccountSettingsRouteScreen
import dev.typetype.android.feature.settings.about.AboutScreen
import dev.typetype.android.feature.settings.appearance.AppearanceRoute as AppearanceRouteScreen
import dev.typetype.android.feature.settings.blocked.BlockedSettingsRoute as BlockedSettingsRouteScreen
import dev.typetype.android.feature.settings.diagnostics.DiagnosticsRoute as DiagnosticsRouteScreen
import dev.typetype.android.feature.settings.player.PlayerSettingsRoute as PlayerSettingsRouteScreen
import dev.typetype.android.feature.settings.content.ContentSettingsRoute as ContentSettingsRouteScreen
import dev.typetype.android.feature.settings.privacy.PrivacySettingsRoute as PrivacySettingsRouteScreen
import dev.typetype.android.feature.settings.storage.StorageSettingsRoute as StorageSettingsRouteScreen
import dev.typetype.android.feature.subscriptions.SubscriptionsRoute as SubscriptionsRouteScreen
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AppNavHost(startRoute: Any, mainViewModel: MainViewModel) {
    val navController: NavHostController = rememberNavController()
    val playerHostController = remember { mainViewModel.playerHostController }
    val serverBaseUrl by mainViewModel.currentServerBaseUrl.collectAsStateWithLifecycle()
    val currentServerId by mainViewModel.currentServerId.collectAsStateWithLifecycle()
    val currentProfile by mainViewModel.currentProfile.collectAsStateWithLifecycle()
    val notificationBadge by rememberNotificationBadge()
    val deArrowViewModel = hiltViewModel<DeArrowBrandingViewModel>()
    val deArrowSettings by deArrowViewModel.settings.collectAsStateWithLifecycle()
    val deArrowEnvironment = remember(deArrowSettings, deArrowViewModel) {
        DeArrowBrandingEnvironment(
            enabled = deArrowSettings.deArrowEnabled,
            preferences = DeArrowPreferences(
                titleMode = deArrowSettings.deArrowTitleMode,
                thumbnailMode = deArrowSettings.deArrowThumbnailMode,
                trustMode = deArrowSettings.deArrowTrustMode,
            ),
            loader = deArrowViewModel::load,
        )
    }
    val onPlayVideo: (String) -> Unit = { videoUrl ->
        playerHostController.openVideo(videoUrl)
    }

    LaunchedEffect(mainViewModel) {
        mainViewModel.events.collectLatest { event ->
            when (event) {
                MainEvent.NavigateToWelcome -> {
                    playerHostController.hide()
                    navController.navigate(WelcomeRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                is MainEvent.NavigateToLogin -> {
                    playerHostController.hide()
                    navController.navigate(
                        LoginRoute(
                            serverId = event.serverId,
                        ),
                    ) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    val onOpenChannel: (String) -> Unit = { channelUrl ->
        navController.navigate(ChannelRoute(channelUrl = channelUrl)) {
            launchSingleTop = true
        }
    }

    val avatarUrl = remember(currentProfile, serverBaseUrl) {
        val p = currentProfile ?: return@remember null
        resolveProfileAvatarUrl(
            serverBaseUrl = serverBaseUrl,
            avatarUrl = p.avatarUrl,
            avatarType = p.avatarType,
            avatarCode = p.avatarCode,
            fallbackSeed = "${p.id}:${p.publicUsername}",
        )
    }
    val avatarFallback = currentProfile?.publicUsername?.firstOrNull()?.toString()

    CompositionLocalProvider(
        LocalServerBaseUrl provides serverBaseUrl,
        LocalDeArrowBranding provides deArrowEnvironment,
    ) {
    AppShell(
        navController = navController,
        playerHostController = playerHostController,
        onOpenSearch = { navController.navigate(SearchRoute) },
        onOpenNotifications = { navController.navigate(NotificationsRoute) },
        onOpenSettings = { navController.navigate(SettingsRoute) },
        onOpenAccounts = { navController.navigate(AccountsRoute) },
        onOpenProfile = { navController.navigate(ProfileSettingsRoute) },
        notificationsAvailable = notificationBadge.isAvailable,
        unreadNotificationsCount = notificationBadge.unreadCount,
        avatarUrl = avatarUrl,
        avatarFallbackLetter = avatarFallback,
        onPlayVideo = onPlayVideo,
        onOpenChannel = onOpenChannel,
        onClosePlayback = mainViewModel::closePlayback,
    ) { innerModifier ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = innerModifier,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(280),
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(280),
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(280),
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(280),
                )
            },
        ) {
            setupDestinations(navController, mainViewModel)
            composable<HomeRoute> {
                HomeRouteScreen(
                    viewModel = hiltViewModel<HomeViewModel>(),
                    onPlayVideo = onPlayVideo,
                    onOpenChannel = { channelUrl ->
                        navController.navigate(ChannelRoute(channelUrl = channelUrl)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable<SearchRoute> {
                SearchRouteScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPlayVideo = onPlayVideo,
                    onOpenChannel = { channelUrl ->
                        navController.navigate(ChannelRoute(channelUrl = channelUrl)) {
                            launchSingleTop = true
                        }
                    },
                    onOpenPlaylist = { playlistUrl ->
                        navController.navigate(PublicPlaylistRoute(playlistUrl)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            notificationsDestination(navController, onPlayVideo)
            composable<SubscriptionsRoute> {
                SubscriptionsRouteScreen(
                    onPlayVideo = onPlayVideo,
                    onOpenChannel = { channelUrl ->
                        navController.navigate(ChannelRoute(channelUrl = channelUrl)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            libraryDestination(
                navController = navController,
                onPlayVideo = onPlayVideo,
                onOpenChannel = onOpenChannel,
            )
            composable<PlaylistRoute> {
                PlaylistRouteScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPlayVideo = onPlayVideo,
                    onPlayQueue = { title, videos, shuffle ->
                        playerHostController.openQueue(
                            title = title,
                            entries = videos.map { it.toPlaybackQueueEntry() },
                            shuffle = shuffle,
                        )
                    },
                    onOpenChannel = onOpenChannel,
                )
            }
            publicPlaylistDestination(
                navController = navController,
                playerHostController = playerHostController,
                onPlayVideo = onPlayVideo,
                onOpenChannel = onOpenChannel,
            )
            settingsDestinations(
                navController = navController,
                importsAvailable = currentProfile?.id?.startsWith("guest:") == false,
                youtubeSessionAvailable = currentProfile?.id?.startsWith("guest:") == false,
                onSignOut = mainViewModel::signOut,
            )
            composable<AccountsRoute> {
                AccountSettingsRouteScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onAccountActivated = {
                        mainViewModel.onAccountActivated()
                        navController.navigate(HomeRoute) {
                            popUpTo(HomeRoute) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onSignIn = { serverId, accountId ->
                        navController.navigate(LoginRoute(serverId, accountId))
                    },
                    onAddInstance = { navController.navigate(AddServerRoute) },
                )
            }
            profileDestinations(navController, currentServerId)
            composable<AppearanceRoute> {
                AppearanceRouteScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable<ContentSettingsRoute> {
                ContentSettingsRouteScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable<PlayerSettingsRoute> {
                PlayerSettingsRouteScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable<StorageSettingsRoute> {
                StorageSettingsRouteScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable<PrivacySettingsRoute> {
                PrivacySettingsRouteScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable<DiagnosticsRoute> {
                DiagnosticsRouteScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable<BlockedSettingsRoute> {
                BlockedSettingsRouteScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable<AboutRoute> {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            channelAndPodcastDestinations(
                navController = navController,
                playerHostController = playerHostController,
                onPlayVideo = onPlayVideo,
                onOpenChannel = onOpenChannel,
            )
        }
    }
    }
}
