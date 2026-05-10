package dev.typetype.android.feature.channel

sealed interface ChannelAction {
    data object OnRefresh : ChannelAction
    data object OnToggleSubscribe : ChannelAction
}
