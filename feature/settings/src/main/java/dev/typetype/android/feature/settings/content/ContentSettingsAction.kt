package dev.typetype.android.feature.settings.content

import dev.typetype.android.domain.usersettings.UserSettings

sealed interface ContentSettingsAction {
    data object Retry : ContentSettingsAction
    data object DismissFailure : ContentSettingsAction

    sealed interface Update : ContentSettingsAction

    data class SetDefaultLandingPage(val page: String) : Update
    data class SetHideHomeRecommendations(val hidden: Boolean) : Update
    data class SetHideContinueWatching(val hidden: Boolean) : Update
    data class SetHideRelatedVideos(val hidden: Boolean) : Update
    data class SetHideComments(val hidden: Boolean) : Update
    data class SetHideShorts(val hidden: Boolean) : Update
    data class SetHideSubscriptionLiveStreams(val hidden: Boolean) : Update
    data class SetDeArrowEnabled(val enabled: Boolean) : Update
    data class SetDeArrowTitleMode(val mode: String) : Update
    data class SetDeArrowThumbnailMode(val mode: String) : Update
    data class SetDeArrowTrustMode(val mode: String) : Update
}

internal fun UserSettings.updatedBy(action: ContentSettingsAction.Update): UserSettings =
    when (action) {
        is ContentSettingsAction.SetDefaultLandingPage -> copy(defaultLandingPage = action.page)
        is ContentSettingsAction.SetHideHomeRecommendations ->
            copy(hideHomeRecommendations = action.hidden)
        is ContentSettingsAction.SetHideContinueWatching ->
            copy(hideContinueWatching = action.hidden)
        is ContentSettingsAction.SetHideRelatedVideos -> copy(hideRelatedVideos = action.hidden)
        is ContentSettingsAction.SetHideComments -> copy(hideComments = action.hidden)
        is ContentSettingsAction.SetHideShorts -> copy(hideShorts = action.hidden)
        is ContentSettingsAction.SetHideSubscriptionLiveStreams ->
            copy(hideSubscriptionLiveStreams = action.hidden)
        is ContentSettingsAction.SetDeArrowEnabled -> copy(deArrowEnabled = action.enabled)
        is ContentSettingsAction.SetDeArrowTitleMode -> copy(deArrowTitleMode = action.mode)
        is ContentSettingsAction.SetDeArrowThumbnailMode -> copy(deArrowThumbnailMode = action.mode)
        is ContentSettingsAction.SetDeArrowTrustMode -> copy(deArrowTrustMode = action.mode)
    }
