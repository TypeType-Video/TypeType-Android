package dev.typetype.android.feature.settings.rss

import dev.typetype.android.domain.rss.RssFeedScope

sealed interface RssFeedsAction {
    data object Retry : RssFeedsAction
    data object Create : RssFeedsAction
    data class Edit(val feedId: String) : RssFeedsAction
    data object DismissEditor : RssFeedsAction
    data class SetName(val name: String) : RssFeedsAction
    data class SetScope(val scope: RssFeedScope) : RssFeedsAction
    data class ToggleChannel(val channelUrl: String) : RssFeedsAction
    data class ToggleService(val serviceId: Int) : RssFeedsAction
    data class SetVideos(val included: Boolean) : RssFeedsAction
    data class SetShorts(val included: Boolean) : RssFeedsAction
    data class SetLive(val included: Boolean) : RssFeedsAction
    data class SetUpcoming(val included: Boolean) : RssFeedsAction
    data object Save : RssFeedsAction
    data class SetEnabled(val feedId: String, val enabled: Boolean) : RssFeedsAction
    data class RequestRegenerate(val feedId: String) : RssFeedsAction
    data object DismissRegenerate : RssFeedsAction
    data object ConfirmRegenerate : RssFeedsAction
    data class RequestDelete(val feedId: String) : RssFeedsAction
    data object DismissDelete : RssFeedsAction
    data object ConfirmDelete : RssFeedsAction
    data object DismissSecret : RssFeedsAction
    data object DismissFailure : RssFeedsAction
}
