package dev.typetype.android

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import dev.typetype.android.core.ui.navigation.HomeRoute
import dev.typetype.android.core.ui.navigation.LibraryRoute
import dev.typetype.android.core.ui.navigation.SubscriptionsRoute
import dev.typetype.android.core.ui.components.ProfileAvatar

internal data class TopLevelTab(
    val route: Any,
    val labelRes: Int,
    val iconRes: Int,
)

internal val topLevelTabs = listOf(
    TopLevelTab(HomeRoute, R.string.tab_home, R.drawable.ic_home),
    TopLevelTab(SubscriptionsRoute, R.string.tab_subscriptions, R.drawable.ic_subscriptions),
    TopLevelTab(LibraryRoute, R.string.tab_library, R.drawable.ic_library),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppTopBar(
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    avatarUrl: String?,
    avatarFallbackLetter: String?,
) {
    androidx.compose.foundation.layout.Column {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_typetype_brand),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.about_app_name).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.sp,
                        ),
                    )
                }
            },
            actions = {
                IconButton(onClick = onOpenSearch) {
                    Icon(Icons.Filled.Search, stringResource(R.string.search_submit))
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, stringResource(R.string.settings_title))
                }
                ProfileAvatarButton(avatarUrl, avatarFallbackLetter, onOpenProfile)
                Spacer(Modifier.width(4.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
            ),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
internal fun AppBottomBar(
    currentDestination: NavDestination?,
    fallbackTabRouteQualifiedName: String?,
    onTabClick: (Any) -> Unit,
) {
    androidx.compose.foundation.layout.Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        NavigationBar(
            modifier = Modifier.testTag(APP_BOTTOM_NAVIGATION_TAG),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            topLevelTabs.forEach { tab ->
                val selected = tab.isSelected(currentDestination, fallbackTabRouteQualifiedName)
                NavigationBarItem(
                    selected = selected,
                    onClick = { if (!currentDestination.matchesRoute(tab.route)) onTabClick(tab.route) },
                    icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                    label = { Text(stringResource(tab.labelRes)) },
                )
            }
        }
    }
}

@Composable
internal fun AppNavigationRail(
    currentDestination: NavDestination?,
    fallbackTabRouteQualifiedName: String?,
    onTabClick: (Any) -> Unit,
) {
    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.systemBars)
            .testTag(APP_NAVIGATION_RAIL_TAG),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Spacer(Modifier.weight(1f))
        topLevelTabs.forEach { tab ->
            val selected = tab.isSelected(currentDestination, fallbackTabRouteQualifiedName)
            NavigationRailItem(
                selected = selected,
                onClick = { if (!currentDestination.matchesRoute(tab.route)) onTabClick(tab.route) },
                icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                label = { Text(stringResource(tab.labelRes)) },
            )
        }
        Spacer(Modifier.weight(1f))
    }
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
        ProfileAvatar(
            imageUrl = avatarUrl,
            fallbackLetter = fallbackLetter,
            contentDescription = stringResource(R.string.settings_profile_title),
            modifier = Modifier.fillMaxSize().padding(3.dp),
        )
    }
}

private fun TopLevelTab.isSelected(
    destination: NavDestination?,
    fallbackRouteName: String?,
): Boolean {
    val direct = destination.matchesRoute(route)
    val anyDirect = topLevelTabs.any { destination.matchesRoute(it.route) }
    return direct || (!anyDirect && route::class.qualifiedName == fallbackRouteName)
}

internal fun NavDestination?.matchesRoute(route: Any): Boolean {
    val destination = this ?: return false
    return when (route) {
        HomeRoute -> destination.hasRoute<HomeRoute>()
        SubscriptionsRoute -> destination.hasRoute<SubscriptionsRoute>()
        LibraryRoute -> destination.hasRoute<LibraryRoute>()
        else -> false
    }
}

internal const val APP_BOTTOM_NAVIGATION_TAG = "app-bottom-navigation"
internal const val APP_NAVIGATION_RAIL_TAG = "app-navigation-rail"
