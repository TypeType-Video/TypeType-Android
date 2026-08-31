package video.typetype.tv.ui

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.focus.FocusRequester
import video.typetype.sdk.core.ServiceId
import video.typetype.sdk.core.UserSettings
import video.typetype.tv.data.displayName

internal fun LazyListScope.playbackPreferenceItems(
    settings: UserSettings,
    services: List<ServiceId>,
    selectedService: ServiceId,
    initialFocus: FocusRequester,
    rightExit: FocusRequester,
    onServiceChange: (ServiceId) -> Unit,
    onChange: (UserSettings) -> Unit,
) {
    if (services.size > 1) {
        item { SettingsSectionTitle("Default service", "The service selected when TypeType starts.") }
        item {
            OptionRow(services.map(ServiceId::displayName), selectedService.displayName(), rightExit, initialFocus) { label ->
                services.firstOrNull { it.displayName() == label }?.let(onServiceChange)
            }
        }
    }
    item { SettingsSectionTitle("Playback") }
    item {
        ToggleRow(
            "Autoplay next video",
            "Continue with the next recommendation when playback ends.",
            settings.autoplay,
            rightExit,
            if (services.size > 1) null else initialFocus,
        ) {
            onChange(settings.copy(autoplay = it))
        }
    }
    if (settings.autoplay) {
        item { SettingsSectionTitle("Autoplay countdown", "Choose how long the Up next screen remains visible.") }
        item {
            val delays = listOf(0, 5, 10, 15, 30, 60)
            OptionRow(
                delays.map(Int::toDelayLabel),
                settings.autoplayCountdownSeconds.coerceIn(0, 60).toDelayLabel(),
                rightExit,
            ) { label ->
                delays.firstOrNull { it.toDelayLabel() == label }?.let {
                    onChange(settings.copy(autoplayCountdownSeconds = it))
                }
            }
        }
    }
    item {
        ToggleRow("Prefer original audio", "Choose the creator's original track when it is available.", settings.preferOriginalLanguage, rightExit) {
            onChange(settings.copy(preferOriginalLanguage = it))
        }
    }
    item { SettingsSectionTitle("Default quality") }
    item {
        val qualities = listOf("Auto", "2160p", "1440p", "1080p", "720p", "480p")
        OptionRow(qualities, settings.defaultQuality.ifBlank { "Auto" }, rightExit) { value ->
            onChange(settings.copy(defaultQuality = value.takeUnless { it == "Auto" }.orEmpty()))
        }
    }
    item { SettingsSectionTitle("Playback speed") }
    item {
        val speeds = listOf(.5, .75, 1.0, 1.25, 1.5, 2.0)
        OptionRow(speeds.map(Double::toSpeedLabel), settings.defaultPlaybackSpeed.toSpeedLabel(), rightExit) { label ->
            speeds.firstOrNull { it.toSpeedLabel() == label }?.let { onChange(settings.copy(defaultPlaybackSpeed = it)) }
        }
    }
}

internal fun LazyListScope.contentPreferenceItems(
    settings: UserSettings,
    initialFocus: FocusRequester,
    rightExit: FocusRequester,
    onChange: (UserSettings) -> Unit,
) {
    item { SettingsSectionTitle("Home", "Keep the rows that matter to you.") }
    preference(
        "Recommendations",
        "Show personalized recommendations on Home.",
        !settings.hideHomeRecommendations,
        rightExit,
        initialFocus,
    ) {
        onChange(settings.copy(hideHomeRecommendations = !it))
    }
    preference("Continue watching", "Keep unfinished videos visible on Home.", !settings.hideContinueWatching, rightExit) {
        onChange(settings.copy(hideContinueWatching = !it))
    }
    preference("Shorts", "Include vertical videos in discovery.", !settings.hideShorts, rightExit) {
        onChange(settings.copy(hideShorts = !it))
    }
    item { SettingsSectionTitle("Video details") }
    preference("Related videos", "Show more videos after the current selection.", !settings.hideRelatedVideos, rightExit) {
        onChange(settings.copy(hideRelatedVideos = !it))
    }
    preference("Comments", "Load comments on video details and in the player.", !settings.hideComments, rightExit) {
        onChange(settings.copy(hideComments = !it))
    }
    item { SettingsSectionTitle("Subscriptions") }
    preference("Live streams", "Include live streams in subscription feeds.", !settings.hideSubscriptionLiveStreams, rightExit) {
        onChange(settings.copy(hideSubscriptionLiveStreams = !it))
    }
    preference("Members-only videos", "Include restricted videos when your account can access them.", !settings.hideMembersOnlyContent, rightExit) {
        onChange(settings.copy(hideMembersOnlyContent = !it))
    }
    item { SettingsSectionTitle("Presentation") }
    preference("Alternative titles and thumbnails", "Use DeArrow community titles and thumbnails when available.", settings.deArrowEnabled, rightExit) {
        onChange(settings.copy(deArrowEnabled = it))
    }
    item { SettingsSectionTitle("History") }
    preference("Watch history", "Save playback progress to this TypeType account.", !settings.disableWatchHistory, rightExit) {
        onChange(settings.copy(disableWatchHistory = !it))
    }
}

private fun LazyListScope.preference(
    title: String,
    description: String,
    checked: Boolean,
    rightExit: FocusRequester,
    initialFocus: FocusRequester? = null,
    onChange: (Boolean) -> Unit,
) {
    item { ToggleRow(title, description, checked, rightExit, initialFocus, onChange) }
}

private fun Double.toSpeedLabel(): String = if (this == 1.0) "Normal" else "${this}x"

private fun Int.toDelayLabel(): String = if (this == 0) "Immediately" else "${this}s"
