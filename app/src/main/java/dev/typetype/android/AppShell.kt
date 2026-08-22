package dev.typetype.android

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.core.ui.components.LocalAppSnackbarHost
import dev.typetype.android.core.ui.navigation.ChannelRoute
import dev.typetype.android.core.ui.navigation.NotificationsRoute
import dev.typetype.android.core.ui.navigation.PlaylistRoute
import dev.typetype.android.core.ui.navigation.PodcastRoute
import dev.typetype.android.core.ui.navigation.PublicPlaylistRoute
import dev.typetype.android.core.ui.navigation.SearchRoute
import dev.typetype.android.core.ui.navigation.ShortsRoute
import dev.typetype.android.feature.player.components.LocalMediaController
import dev.typetype.android.feature.player.components.rememberMediaController
import dev.typetype.android.feature.player.host.PlayerHost
import dev.typetype.android.feature.player.host.PlayerHostController
import dev.typetype.android.feature.player.host.PlayerHostTarget

private const val NAV_BAR_HEIGHT_DP = 80f
private val WIDE_NAVIGATION_THRESHOLD = 600.dp

@Composable
fun AppShell(
    navController: NavHostController,
    playerHostController: PlayerHostController,
    onOpenSettings: () -> Unit,
    onOpenAccounts: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSearch: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    notificationsAvailable: Boolean = false,
    unreadNotificationsCount: Int = 0,
    avatarUrl: String? = null,
    avatarFallbackLetter: String? = null,
    showShorts: Boolean = true,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenChannel: (channelUrl: String) -> Unit,
    onClosePlayback: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isShorts = currentDestination.matchesRoute(ShortsRoute)
    val navigationTabs = visibleTopLevelTabs(showShorts)
    val isTopLevel = topLevelTabs.any { currentDestination.matchesRoute(it.route) }
    val showsNavigation = !isShorts && (
        isTopLevel ||
            currentDestination?.hasRoute<ChannelRoute>() == true ||
            currentDestination?.hasRoute<PlaylistRoute>() == true ||
            currentDestination?.hasRoute<PublicPlaylistRoute>() == true ||
            currentDestination?.hasRoute<PodcastRoute>() == true ||
            currentDestination?.hasRoute<SearchRoute>() == true ||
            currentDestination?.hasRoute<NotificationsRoute>() == true
    )
    var activeTabRoute by rememberSaveable { mutableStateOf<String?>(null) }
    var isPlayerFullscreen by remember { mutableStateOf(false) }
    var playerTransitionProgress by remember { mutableStateOf(0f) }
    val playerHostState by playerHostController.state.collectAsStateWithLifecycle()
    val appChromeVisible = isAppChromeVisible(playerHostState.target, isPlayerFullscreen)
    val phoneChromeAlpha = playerPhoneChromeAlpha(
        hasVideo = playerHostState.videoUrl != null,
        playerTarget = playerHostState.target,
        isPlayerFullscreen = isPlayerFullscreen,
        transitionProgress = playerTransitionProgress,
    )
    LaunchedEffect(currentDestination) {
        topLevelTabs.firstOrNull { currentDestination.matchesRoute(it.route) }?.let {
            activeTabRoute = it.route::class.qualifiedName
        }
    }

    val mediaController = rememberMediaController().value
    val snackbarHostState = remember { SnackbarHostState() }
    CompositionLocalProvider(
        LocalMediaController provides mediaController,
        LocalAppSnackbarHost provides snackbarHostState,
    ) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val usesNavigationRail =
                minOf(maxWidth, maxHeight) >= WIDE_NAVIGATION_THRESHOLD
            Row(modifier = Modifier.fillMaxSize()) {
                if (usesNavigationRail && showsNavigation && appChromeVisible) {
                    AppNavigationRail(
                        currentDestination = currentDestination,
                        fallbackTabRouteQualifiedName = activeTabRoute,
                        onTabClick = navController::navigateTopLevel,
                        tabs = navigationTabs,
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    Scaffold(
                        contentWindowInsets = if (isPlayerFullscreen || isShorts) {
                            WindowInsets(0)
                        } else {
                            WindowInsets.systemBars
                        },
                        topBar = {
                            if (isTopLevel && !isShorts && !usesNavigationRail) {
                                AppTopBar(
                                    onOpenSearch = onOpenSearch,
                                    onOpenNotifications = onOpenNotifications,
                                    onOpenSettings = onOpenSettings,
                                    onOpenProfile = onOpenProfile,
                                    notificationsAvailable = notificationsAvailable,
                                    unreadNotificationsCount = unreadNotificationsCount,
                                    avatarUrl = avatarUrl,
                                    avatarFallbackLetter = avatarFallbackLetter,
                                    modifier = Modifier.playerChrome(phoneChromeAlpha),
                                )
                            } else if (isTopLevel && !isShorts && appChromeVisible) {
                                AppTopBar(
                                    onOpenSearch = onOpenSearch,
                                    onOpenNotifications = onOpenNotifications,
                                    onOpenSettings = onOpenSettings,
                                    onOpenProfile = onOpenProfile,
                                    notificationsAvailable = notificationsAvailable,
                                    unreadNotificationsCount = unreadNotificationsCount,
                                    avatarUrl = avatarUrl,
                                    avatarFallbackLetter = avatarFallbackLetter,
                                )
                            }
                        },
                        bottomBar = {
                            if (!usesNavigationRail && showsNavigation) {
                                AppBottomBar(
                                    currentDestination = currentDestination,
                                    fallbackTabRouteQualifiedName = activeTabRoute,
                                    onTabClick = navController::navigateTopLevel,
                                    tabs = navigationTabs,
                                    modifier = Modifier.playerChrome(phoneChromeAlpha),
                                )
                            }
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                    ) { padding ->
                        content(
                            Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(
                                    bottom = if (playerHostState.target == PlayerHostTarget.Mini) {
                                        64.dp
                                    } else {
                                        0.dp
                                    },
                                ),
                        )
                    }
                    PlayerHost(
                        controller = playerHostController,
                        bottomBarHeightDp = if (
                            !usesNavigationRail && showsNavigation
                        ) {
                            NAV_BAR_HEIGHT_DP
                        } else {
                            0f
                        },
                        isFullscreen = isPlayerFullscreen,
                        onFullscreenChange = { isPlayerFullscreen = it },
                        mediaController = mediaController,
                        onOpenChannel = onOpenChannel,
                        onOpenAccounts = onOpenAccounts,
                        onClosePlayback = onClosePlayback,
                        onTransitionProgressChange = { playerTransitionProgress = it },
                        content = {},
                    )
                }
            }
        }
    }
}

private fun Modifier.playerChrome(alpha: Float): Modifier =
    graphicsLayer { this.alpha = alpha.coerceIn(0f, 1f) }
        .then(if (alpha <= 0f) Modifier.clearAndSetSemantics { } else Modifier)

internal fun isAppChromeVisible(
    playerTarget: PlayerHostTarget,
    isPlayerFullscreen: Boolean,
): Boolean = playerTarget != PlayerHostTarget.Expanded && !isPlayerFullscreen

internal fun playerPhoneChromeAlpha(
    hasVideo: Boolean,
    playerTarget: PlayerHostTarget,
    isPlayerFullscreen: Boolean,
    transitionProgress: Float,
): Float = when {
    isPlayerFullscreen -> 0f
    hasVideo && playerTarget != PlayerHostTarget.Embedded -> transitionProgress.coerceIn(0f, 1f)
    isAppChromeVisible(playerTarget, false) -> 1f
    else -> 0f
}

private fun NavHostController.navigateTopLevel(route: Any) {
    if (currentDestination?.hasRoute<SearchRoute>() == true) {
        popBackStack()
    }
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
