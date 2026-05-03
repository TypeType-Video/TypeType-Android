package dev.typetype.android

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
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
import dev.typetype.android.core.ui.navigation.HomeRoute
import dev.typetype.android.core.ui.navigation.LibraryRoute
import dev.typetype.android.core.ui.navigation.PlayerRoute
import dev.typetype.android.core.ui.navigation.SubscriptionsRoute
import dev.typetype.android.feature.player.components.LocalMediaController
import dev.typetype.android.feature.player.components.MiniPlayerBar
import dev.typetype.android.feature.player.components.rememberMediaController

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    navController: NavHostController,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit = {},
    onPlayVideo: (videoUrl: String) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isTopLevel = topLevelTabs.any { tab ->
        currentDestination.matchesRoute(tab.route)
    }
    val isPlayer = currentDestination?.hasRoute<PlayerRoute>() == true

    val controllerState = rememberMediaController()
    val controller = controllerState.value

    CompositionLocalProvider(LocalMediaController provides controller) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (isTopLevel) {
                    AppTopBar(onOpenSearch = onOpenSearch, onOpenSettings = onOpenSettings)
                }
            },
            bottomBar = {
                if (isTopLevel) {
                    Column {
                        if (controller != null && !isPlayer) {
                            val item = controller.currentMediaItem
                            if (item != null) {
                                MiniPlayerBar(
                                    player = controller,
                                    title = item.mediaMetadata.title?.toString() ?: "",
                                    subtitle = item.mediaMetadata.artist?.toString() ?: "",
                                    artworkUri = item.mediaMetadata.artworkUri?.toString(),
                                    onExpand = {
                                        val mediaId = item.mediaId
                                        if (mediaId.isNotBlank()) {
                                            onPlayVideo(mediaId)
                                        }
                                    },
                                    onClose = {
                                        controller.stop()
                                        controller.clearMediaItems()
                                    },
                                )
                            }
                        }
                        AppBottomBar(
                            currentDestination = currentDestination,
                            onTabClick = { route ->
                                navController.navigate(route) {
                                    popUpTo(HomeRoute) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            },
        ) { padding ->
            content(Modifier.fillMaxSize().padding(padding))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(onOpenSearch: () -> Unit, onOpenSettings: () -> Unit) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.ic_launcher_monochrome),
                        contentDescription = null,
                        modifier = androidx.compose.ui.Modifier.size(22.dp),
                    )
                }
                Spacer(androidx.compose.ui.Modifier.width(10.dp))
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
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

@Composable
private fun AppBottomBar(
    currentDestination: NavDestination?,
    onTabClick: (Any) -> Unit,
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        topLevelTabs.forEach { tab ->
            val selected = currentDestination.matchesRoute(tab.route)
            NavigationBarItem(
                selected = selected,
                onClick = { if (!selected) onTabClick(tab.route) },
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
