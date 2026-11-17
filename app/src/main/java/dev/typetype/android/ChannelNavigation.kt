package dev.typetype.android

import dev.typetype.android.core.ui.navigation.TwitchChannelRoute
import dev.typetype.android.domain.twitch.TwitchTarget
import dev.typetype.android.domain.twitch.TwitchUrls
import androidx.navigation.NavHostController
import dev.typetype.android.core.ui.navigation.ChannelRoute
import dev.typetype.android.core.ui.navigation.channelNavigationUrl

internal fun NavHostController.navigateToChannel(channelUrl: String) {
    val twitch = TwitchUrls.parse(channelUrl) as? TwitchTarget.Live
    if (twitch != null) navigate(TwitchChannelRoute(twitch.login)) else navigate(ChannelRoute(channelUrl = channelNavigationUrl(channelUrl)))
}
