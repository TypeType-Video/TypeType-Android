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
import dev.typetype.android.core.ui.navigation.AboutRoute
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
import dev.typetype.android.feature.settings.player.PlayerSettingsRoute as PlayerSettingsRouteScreen
import dev.typetype.android.feature.setup.addserver.AddServerRoute as AddServerRouteScreen
import dev.typetype.android.feature.setup.login.LoginRoute as LoginRouteScreen
import dev.typetype.android.feature.setup.welcome.WelcomeRoute as WelcomeRouteScreen
import dev.typetype.android.feature.subscriptions.SubscriptionsRoute as SubscriptionsRouteScreen
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AppNavHost(startRoute: Any, mainViewModel: MainViewModel) {
    val navController: NavHostController = rememberNavController()
    val playerHostController = remember { mainViewModel.playerHostController }
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

    AppShell(
        navController = navController,
        playerHostController = playerHostController,
        onOpenSearch = { navController.navigate(SearchRoute) },
        onOpenSettings = { navController.navigate(SettingsRoute) },
        onPlayVideo = onPlayVideo,
        onOpenChannel = { channelUrl ->
            navController.navigate(ChannelRoute(channelUrl = channelUrl)) {
                launchSingleTop = true
            }
        },
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
                )
            }
            composable<SearchRoute> {
                SearchRouteScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPlayVideo = onPlayVideo,
                )
            }
            composable<SubscriptionsRoute> {
                SubscriptionsRouteScreen(
                    onPlayVideo = onPlayVideo,
                )
            }
            composable<LibraryRoute> {
                LibraryRouteScreen(
                    onPlayVideo = onPlayVideo,
                    onOpenPlaylist = { playlistId ->
                        navController.navigate(PlaylistRoute(playlistId = playlistId))
                    },
                )
            }
            composable<PlaylistRoute> {
                PlaylistRouteScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPlayVideo = onPlayVideo,
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onOpenAppearance = { navController.navigate(AppearanceRoute) },
                    onOpenPlayer = { navController.navigate(PlayerSettingsRoute) },
                    onOpenAbout = { navController.navigate(AboutRoute) },
                    onSignOut = {
                        navController.popBackStack()
                        mainViewModel.signOut()
                    },
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
