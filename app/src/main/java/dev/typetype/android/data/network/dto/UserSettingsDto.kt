package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserSettingsDto(
    val defaultService: Int = 0,
    val defaultQuality: String = "1080p",
    val autoplay: Boolean = true,
    val skipPlaylistAutoplayScreen: Boolean = false,
    val volume: Double = 1.0,
    val muted: Boolean = false,
    val subtitlesEnabled: Boolean = false,
    val defaultSubtitleLanguage: String = "",
    val defaultAudioLanguage: String = "",
    val preferOriginalLanguage: Boolean = false,
    val hideHomeRecommendations: Boolean = false,
    val hideContinueWatching: Boolean = false,
    val disableWatchHistory: Boolean = false,
)
