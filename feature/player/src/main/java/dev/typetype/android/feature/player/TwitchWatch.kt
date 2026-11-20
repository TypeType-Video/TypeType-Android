package dev.typetype.android.feature.player

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.common.Player
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.twitch.TwitchTarget
import dev.typetype.android.domain.twitch.TwitchUrls
import dev.typetype.android.feature.player.livechat.LiveChatPanel
import dev.typetype.android.feature.player.livechat.TwitchVodChatPanel

// A Twitch live or past broadcast is watched with its chat docked under the video (beside it on a wide screen); for a
// live the details above it shrink to what a live needs.
internal class TwitchWatch(val liveLogin: String?, val vod: Boolean) {
    fun dockedChat(videoUrl: String, stream: Stream, player: Player?): (@Composable (Modifier) -> Unit)? = when {
        liveLogin != null -> { modifier -> LiveChatPanel(videoUrl, modifier.navigationBarsPadding().imePadding()) }
        vod -> { modifier -> TwitchVodChatPanel(videoUrl, stream.uploaderUrl, player, modifier.navigationBarsPadding()) }
        else -> null
    }
}

internal fun twitchWatch(videoUrl: String, stream: Stream): TwitchWatch = when (val target = TwitchUrls.parse(videoUrl)) {
    is TwitchTarget.Live -> TwitchWatch(target.login.takeIf { stream.isLive }, vod = false)
    is TwitchTarget.Vod -> TwitchWatch(null, vod = true)
    else -> TwitchWatch(null, vod = false)
}
