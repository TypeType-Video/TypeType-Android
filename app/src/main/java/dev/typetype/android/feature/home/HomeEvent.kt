package dev.typetype.android.feature.home

sealed interface HomeEvent {
    data object NavigateToWelcome : HomeEvent
}
