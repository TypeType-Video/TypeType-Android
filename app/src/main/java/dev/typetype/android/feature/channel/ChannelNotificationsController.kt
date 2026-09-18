package dev.typetype.android.feature.channel

import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.domain.push.ChannelNotificationsPreference
import dev.typetype.android.domain.push.PushRepository
import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject

class ChannelNotificationsController @Inject constructor(
    private val pushRepository: PushRepository,
    private val activeAccountScope: ActiveAccountScope,
    private val serverRepository: ServerRepository,
) {
    suspend fun isAvailable(isSubscribed: Boolean): Boolean {
        if (!isSubscribed) return false
        val scope = runCatching { activeAccountScope.require() }.getOrNull() ?: return false
        return serverRepository.getServer(scope.serverId)?.push?.enabled == true
    }

    suspend fun currentEnabled(channelUrl: String): Boolean? =
        pushRepository.channelPreferences().getOrNull()
            ?.firstOrNull { it.matches(channelUrl) }
            ?.enabled

    suspend fun setEnabled(channelUrl: String, enabled: Boolean): Boolean? {
        pushRepository.setChannelPreference(channelUrl, enabled).getOrNull() ?: return null
        return enabled
    }

    private fun ChannelNotificationsPreference.matches(channelUrl: String): Boolean =
        this.channelUrl == channelUrl ||
            channelUrl.endsWith(this.channelUrl) ||
            this.channelUrl.endsWith(channelUrl)
}
