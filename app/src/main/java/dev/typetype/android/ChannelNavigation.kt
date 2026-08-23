package dev.typetype.android

import androidx.navigation.NavHostController
import dev.typetype.android.core.ui.navigation.ChannelRoute
import dev.typetype.android.core.ui.navigation.channelNavigationUrl

internal fun NavHostController.navigateToChannel(channelUrl: String) {
    navigate(ChannelRoute(channelUrl = channelNavigationUrl(channelUrl)))
}
