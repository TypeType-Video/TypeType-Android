package video.typetype.tv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import video.typetype.tv.data.TvDestination

internal class TvNavigationFocus private constructor(
    private val homeTab: FocusRequester,
    private val searchTab: FocusRequester,
    private val libraryTab: FocusRequester,
    private val settingsTab: FocusRequester,
    val homeHero: FocusRequester,
    val searchField: FocusRequester,
    val libraryContent: FocusRequester,
    val settingsContent: FocusRequester,
) {
    fun tabFor(destination: TvDestination): FocusRequester = when (destination) {
        TvDestination.HOME -> homeTab
        TvDestination.SEARCH -> searchTab
        TvDestination.LIBRARY -> libraryTab
        TvDestination.SETTINGS -> settingsTab
    }

    fun contentFor(destination: TvDestination): FocusRequester? = when (destination) {
        TvDestination.HOME -> homeHero
        TvDestination.SEARCH -> searchField
        TvDestination.LIBRARY -> libraryContent
        TvDestination.SETTINGS -> settingsContent
    }

    companion object {
        @Composable
        fun remember(): TvNavigationFocus = remember {
            TvNavigationFocus(
                homeTab = FocusRequester(),
                searchTab = FocusRequester(),
                libraryTab = FocusRequester(),
                settingsTab = FocusRequester(),
                homeHero = FocusRequester(),
                searchField = FocusRequester(),
                libraryContent = FocusRequester(),
                settingsContent = FocusRequester(),
            )
        }
    }
}
