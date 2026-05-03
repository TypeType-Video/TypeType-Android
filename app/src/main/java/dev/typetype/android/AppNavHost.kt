package dev.typetype.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.typetype.android.core.ui.navigation.AddServerRoute
import dev.typetype.android.core.ui.navigation.AppearanceRoute
import dev.typetype.android.core.ui.navigation.HomeRoute
import dev.typetype.android.core.ui.navigation.LibraryRoute
import dev.typetype.android.core.ui.navigation.LoginRoute
import dev.typetype.android.core.ui.navigation.PlayerRoute
import dev.typetype.android.core.ui.navigation.SettingsRoute
import dev.typetype.android.core.ui.navigation.SubscriptionsRoute
import dev.typetype.android.core.ui.navigation.WelcomeRoute
import dev.typetype.android.feature.home.HomeRoute as HomeRouteScreen
import dev.typetype.android.feature.home.HomeViewModel
import dev.typetype.android.feature.library.LibraryScreen
import dev.typetype.android.feature.player.PlayerRoute as PlayerRouteScreen
import dev.typetype.android.feature.settings.SettingsScreen
import dev.typetype.android.feature.settings.appearance.AppearanceRoute as AppearanceRouteScreen
import dev.typetype.android.feature.setup.addserver.AddServerRoute as AddServerRouteScreen
import dev.typetype.android.feature.setup.login.LoginRoute as LoginRouteScreen
import dev.typetype.android.feature.setup.welcome.WelcomeRoute as WelcomeRouteScreen
import dev.typetype.android.feature.subscriptions.SubscriptionsScreen
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AppNavHost(startRoute: Any, mainViewModel: MainViewModel) {
    val navController: NavHostController = rememberNavController()

    LaunchedEffect(mainViewModel) {
        mainViewModel.events.collectLatest { event ->
            when (event) {
                MainEvent.NavigateToWelcome -> navController.navigate(WelcomeRoute) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    AppShell(
        navController = navController,
        onOpenSettings = { navController.navigate(SettingsRoute) },
    ) { innerModifier ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = innerModifier,
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
                    onPlayVideo = { videoUrl ->
                        navController.navigate(PlayerRoute(videoUrl = videoUrl))
                    },
                )
            }
            composable<SubscriptionsRoute> { SubscriptionsScreen() }
            composable<LibraryRoute> { LibraryScreen() }
            composable<SettingsRoute> {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onOpenAppearance = { navController.navigate(AppearanceRoute) },
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
            composable<PlayerRoute> {
                PlayerRouteScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}
