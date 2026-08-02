package dev.typetype.android.feature.channel

import dev.typetype.android.domain.channel.ChannelSort

sealed interface ChannelAction {
    data object OnRefresh : ChannelAction
    data object OnLoadMore : ChannelAction
    data object OnLoadMorePlaylists : ChannelAction
    data object OnToggleSubscribe : ChannelAction
    data object OnSubmitSearch : ChannelAction
    data object OnClearSearch : ChannelAction
    data class OnSearchInputChanged(val value: String) : ChannelAction
    data class OnSelectSort(val sort: ChannelSort) : ChannelAction
    data class OnSelectTab(val tab: ChannelTab) : ChannelAction
}
