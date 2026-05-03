package dev.typetype.android.feature.home

sealed interface HomeAction {
    data object OnRefresh : HomeAction
    data object OnSignOutClick : HomeAction
}
