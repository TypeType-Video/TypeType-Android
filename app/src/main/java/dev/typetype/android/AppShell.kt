package dev.typetype.android

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.LocalAppSnackbarHost
import dev.typetype.android.core.ui.navigation.ChannelRoute
import dev.typetype.android.core.ui.navigation.HomeRoute
import dev.typetype.android.core.ui.navigation.LibraryRoute
import dev.typetype.android.core.ui.navigation.PlaylistRoute
import dev.typetype.android.core.ui.navigation.SearchRoute
import dev.typetype.android.core.ui.navigation.SubscriptionsRoute
import dev.typetype.android.feature.player.components.LocalMediaController
import dev.typetype.android.feature.player.components.rememberMediaController
import dev.typetype.android.feature.player.host.PlayerHost
import dev.typetype.android.feature.player.host.PlayerHostController

internal data class TopLevelTab(
    val route: Any,
    val labelRes: Int,
    val iconRes: Int,
)

private val topLevelTabs = listOf(
    TopLevelTab(HomeRoute, R.string.tab_home, R.drawable.ic_home),
    TopLevelTab(SubscriptionsRoute, R.string.tab_subscriptions, R.drawable.ic_subscriptions),
    TopLevelTab(LibraryRoute, R.string.tab_library, R.drawable.ic_library),
)

private val NAV_BAR_HEIGHT_DP: Float = 80f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    navController: NavHostController,
    playerHostController: PlayerHostController,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    avatarUrl: String? = null,
    avatarFallbackLetter: String? = null,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenChannel: (channelUrl: String) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isTopLevel = topLevelTabs.any { tab ->
        currentDestination.matchesRoute(tab.route)
    }
    val showsBottomNav = isTopLevel ||
        currentDestination?.hasRoute<ChannelRoute>() == true ||
        currentDestination?.hasRoute<PlaylistRoute>() == true ||
        currentDestination?.hasRoute<SearchRoute>() == true

    var activeTabRoute by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(currentDestination) {
        val matched = topLevelTabs.firstOrNull { tab ->
            currentDestination.matchesRoute(tab.route)
        }
        if (matched != null) {
            activeTabRoute = matched.route::class.qualifiedName
        }
    }

    val controllerState = rememberMediaController()
    val controller = controllerState.value
    val snackbarHostState = remember { SnackbarHostState() }

    CompositionLocalProvider(
        LocalMediaController provides controller,
        LocalAppSnackbarHost provides snackbarHostState,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.systemBars,
                topBar = {
                    if (isTopLevel) {
                        AppTopBar(
                            onOpenSearch = onOpenSearch,
                            onOpenSettings = onOpenSettings,
                            onOpenProfile = onOpenProfile,
                            avatarUrl = avatarUrl,
                            avatarFallbackLetter = avatarFallbackLetter,
                        )
                    }
                },
                bottomBar = {
                    if (showsBottomNav) {
                        AppBottomBar(
                            currentDestination = currentDestination,
                            fallbackTabRouteQualifiedName = activeTabRoute,
                            onTabClick = { route ->
                                navController.navigate(route) {
                                    popUpTo(HomeRoute) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { padding ->
                content(Modifier.fillMaxSize().padding(padding))
            }

            PlayerHost(
                controller = playerHostController,
                bottomBarHeightDp = if (showsBottomNav) NAV_BAR_HEIGHT_DP else 0f,
                mediaController = controller,
                onOpenChannel = onOpenChannel,
                content = {},
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    avatarUrl: String?,
    avatarFallbackLetter: String?,
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_monochrome),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "TypeType",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        },
        actions = {
            IconButton(onClick = onOpenSearch) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            ProfileAvatarButton(
                avatarUrl = avatarUrl,
                fallbackLetter = avatarFallbackLetter,
                onClick = onOpenProfile,
            )
            Spacer(Modifier.width(4.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

@Composable
private fun ProfileAvatarButton(
    avatarUrl: String?,
    fallbackLetter: String?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(34.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            coil3.compose.AsyncImage(
                model = avatarUrl,
                contentDescription = "Profile",
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(3.dp),
            )
        } else {
            Text(
                text = fallbackLetter?.uppercase() ?: "?",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppBottomBar(
    currentDestination: NavDestination?,
    fallbackTabRouteQualifiedName: String?,
    onTabClick: (Any) -> Unit,
) {
    val anyTopLevelMatched = topLevelTabs.any { tab ->
        currentDestination.matchesRoute(tab.route)
    }
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        topLevelTabs.forEach { tab ->
            val matchesCurrent = currentDestination.matchesRoute(tab.route)
            val matchesFallback = !anyTopLevelMatched &&
                tab.route::class.qualifiedName == fallbackTabRouteQualifiedName
            val selected = matchesCurrent || matchesFallback
            NavigationBarItem(
                selected = selected,
                onClick = { if (!matchesCurrent) onTabClick(tab.route) },
                icon = {
                    Icon(
                        painter = painterResource(tab.iconRes),
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(tab.labelRes)) },
            )
        }
    }
}

private fun NavDestination?.matchesRoute(route: Any): Boolean {
    val target = this ?: return false
    return when (route) {
        HomeRoute -> target.hasRoute<HomeRoute>()
        SubscriptionsRoute -> target.hasRoute<SubscriptionsRoute>()
        LibraryRoute -> target.hasRoute<LibraryRoute>()
        else -> false
    }
}
