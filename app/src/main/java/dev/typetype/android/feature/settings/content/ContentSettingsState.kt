package dev.typetype.android.feature.settings.content

data class ContentSettingsState(
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val hideHomeRecommendations: Boolean = false,
    val hideContinueWatching: Boolean = false,
    val hideRelatedVideos: Boolean = false,
    val hideComments: Boolean = false,
    val hideShorts: Boolean = false,
    val deArrowEnabled: Boolean = false,
    val deArrowTitleMode: String = "dearrow",
    val deArrowThumbnailMode: String = "dearrow_or_random",
    val deArrowTrustMode: String = "accepted",
    val errorMessage: String? = null,
    val errorRequestId: String? = null,
)
