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
import dev.typetype.android.core.ui.navigation.AboutRoute
import dev.typetype.android.core.ui.navigation.BlockedSettingsRoute
import dev.typetype.android.core.ui.navigation.PrivacySettingsRoute
import dev.typetype.android.core.ui.navigation.ProfileSettingsRoute
import dev.typetype.android.core.ui.share.LocalServerBaseUrl
import dev.typetype.android.core.ui.navigation.AddServerRoute
import dev.typetype.android.core.ui.navigation.AppearanceRoute
import dev.typetype.android.core.ui.navigation.ChannelRoute
import dev.typetype.android.core.ui.navigation.HomeRoute
import dev.typetype.android.core.ui.navigation.LibraryRoute
import dev.typetype.android.core.ui.navigation.LoginRoute
import dev.typetype.android.core.ui.navigation.PlayerSettingsRoute
import dev.typetype.android.core.ui.navigation.PlaylistRoute
import dev.typetype.android.core.ui.navigation.SearchRoute
import dev.typetype.android.core.ui.navigation.SettingsRoute
import dev.typetype.android.core.ui.navigation.StorageSettingsRoute
import dev.typetype.android.core.ui.navigation.SubscriptionsRoute
import dev.typetype.android.core.ui.navigation.WelcomeRoute
import dev.typetype.android.feature.channel.ChannelRoute as ChannelRouteScreen
import dev.typetype.android.feature.home.HomeRoute as HomeRouteScreen
import dev.typetype.android.feature.home.HomeViewModel
import dev.typetype.android.feature.library.LibraryRoute as LibraryRouteScreen
import dev.typetype.android.feature.library.playlist.PlaylistRoute as PlaylistRouteScreen
import dev.typetype.android.feature.search.SearchRoute as SearchRouteScreen
import dev.typetype.android.feature.settings.SettingsScreen
import dev.typetype.android.feature.settings.about.AboutScreen
import dev.typetype.android.feature.settings.appearance.AppearanceRoute as AppearanceRouteScreen
import dev.typetype.android.feature.settings.blocked.BlockedSettingsRoute as BlockedSettingsRouteScreen
import dev.typetype.android.feature.settings.player.PlayerSettingsRoute as PlayerSettingsRouteScreen
import dev.typetype.android.feature.settings.privacy.PrivacySettingsRoute as PrivacySettingsRouteScreen
import dev.typetype.android.feature.settings.profile.ProfileSettingsRoute as ProfileSettingsRouteScreen
import dev.typetype.android.feature.settings.storage.StorageSettingsRoute as StorageSettingsRouteScreen
import dev.typetype.android.feature.setup.addserver.AddServerRoute as AddServerRouteScreen
import dev.typetype.android.feature.setup.login.LoginRoute as LoginRouteScreen
import dev.typetype.android.feature.setup.welcome.WelcomeRoute as WelcomeRouteScreen
import dev.typetype.android.feature.subscriptions.SubscriptionsRoute as SubscriptionsRouteScreen
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AppNavHost(startRoute: Any, mainViewModel: MainViewModel) {
    val navController: NavHostController = rememberNavController()
    val playerHostController = remember { mainViewModel.playerHostController }
    val serverBaseUrl by mainViewModel.currentServerBaseUrl.collectAsStateWithLifecycle()
    val currentProfile by mainViewModel.currentProfile.collectAsStateWithLifecycle()
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
                            guestAllowed = false,
                            registrationAllowed = false,
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
        when {
            p.avatarType == "emoji" && p.avatarCode.isNotBlank() ->
                dev.typetype.android.core.openmoji.openMojiUrl(serverBaseUrl, p.avatarCode)
            p.avatarUrl.isNotBlank() && p.avatarUrl.startsWith("http") -> p.avatarUrl
            else -> dev.typetype.android.core.openmoji.openMojiUrl(
                serverBaseUrl,
                dev.typetype.android.core.openmoji.pickOpenMojiCode("${p.id}:${p.publicUsername}"),
            )
        }
    }
    val avatarFallback = currentProfile?.publicUsername?.firstOrNull()?.toString()

    CompositionLocalProvider(LocalServerBaseUrl provides serverBaseUrl) {
    AppShell(
        navController = navController,
        playerHostController = playerHostController,
        onOpenSearch = { navController.navigate(SearchRoute) },
        onOpenSettings = { navController.navigate(SettingsRoute) },
        onOpenProfile = { navController.navigate(ProfileSettingsRoute) },
        avatarUrl = avatarUrl,
        avatarFallbackLetter = avatarFallback,
        onPlayVideo = onPlayVideo,
        onOpenChannel = onOpenChannel,
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
            composable<WelcomeRoute> {
                WelcomeRouteScreen(
                    onNavigateToAddServer = { navController.navigate(AddServerRoute) },
                )
            }
            composable<AddServerRoute> {
                AddServerRouteScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLogin = { serverId, guestAllowed, registrationAllowed ->
                        navController.navigate(
                            LoginRoute(
                                serverId = serverId,
                                guestAllowed = guestAllowed,
                                registrationAllowed = registrationAllowed,
                            ),
                        ) {
                            popUpTo(WelcomeRoute) { inclusive = false }
                        }
                    },
                )
            }
            composable<LoginRoute> {
                LoginRouteScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHome = {
                        navController.navigate(HomeRoute) {
                            popUpTo(WelcomeRoute) { inclusive = true }
                        }
                    },
                )
            }
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
                )
            }
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
            composable<LibraryRoute> {
                LibraryRouteScreen(
                    onPlayVideo = onPlayVideo,
                    onOpenPlaylist = { playlistId ->
                        navController.navigate(PlaylistRoute(playlistId = playlistId))
                    },
                    onOpenChannel = onOpenChannel,
                )
            }
            composable<PlaylistRoute> {
                PlaylistRouteScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPlayVideo = onPlayVideo,
                    onOpenChannel = onOpenChannel,
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onOpenProfile = { navController.navigate(ProfileSettingsRoute) },
                    onOpenAppearance = { navController.navigate(AppearanceRoute) },
                    onOpenPlayer = { navController.navigate(PlayerSettingsRoute) },
                    onOpenStorage = { navController.navigate(StorageSettingsRoute) },
                    onOpenPrivacy = { navController.navigate(PrivacySettingsRoute) },
                    onOpenBlocked = { navController.navigate(BlockedSettingsRoute) },
                    onOpenAbout = { navController.navigate(AboutRoute) },
                    onSignOut = {
                        navController.popBackStack()
                        mainViewModel.signOut()
                    },
                )
            }
            composable<ProfileSettingsRoute> {
                ProfileSettingsRouteScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable<AppearanceRoute> {
                AppearanceRouteScreen(
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
            composable<ChannelRoute> {
                ChannelRouteScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPlayVideo = onPlayVideo,
                )
            }
        }
    }
    }
}
