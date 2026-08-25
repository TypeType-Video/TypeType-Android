package dev.typetype.android.domain.preferences

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    fun observe(): Flow<AppPreferences>
    suspend fun setAccentColor(accentColor: AccentColor)
    suspend fun setAppearancePersonality(personality: AppearancePersonality)
    suspend fun setAppearanceMode(mode: AppearanceMode)
    suspend fun setAppearanceAmoled(enabled: Boolean)
    suspend fun setAppearanceFont(font: AppearanceFont)
    suspend fun setAppearanceMotion(motion: AppearanceMotion)
    suspend fun setMangaPaper(paper: MangaPaper)
    suspend fun setMangaHeadlineMarker(marker: MangaHeadlineMarker)
    suspend fun setMangaScreentone(enabled: Boolean)
    suspend fun setMangaSpeedLines(enabled: Boolean)
    suspend fun setMangaStarburst(enabled: Boolean)
    suspend fun setMangaInkedIcons(enabled: Boolean)
    suspend fun setMangaPanelTilt(enabled: Boolean)
    suspend fun setPlayerDoubleTapSeekEnabled(enabled: Boolean)
    suspend fun setPlayerDoubleTapSeekSeconds(seconds: Int)
    suspend fun setPlayerPreferredCodec(codec: String)
    suspend fun setPlayerSwipeSeekEnabled(enabled: Boolean)
    suspend fun setPlayerSwipeBrightnessVolumeEnabled(enabled: Boolean)
    suspend fun setPlayerPlaybackBrightnessPercent(percent: Int)
    suspend fun setPlayerLongPressSpeedEnabled(enabled: Boolean)
    suspend fun setPlayerAccessibleControlsEnabled(enabled: Boolean)
    suspend fun setPlayerAutoplayEnabled(enabled: Boolean)
    suspend fun setPlayerAutoplayCountdownSeconds(seconds: Int)
    suspend fun setPlayerAudioOnlyPlayback(enabled: Boolean)
    suspend fun setPlayerPauseInBackground(enabled: Boolean)
    suspend fun setDanmakuEnabled(enabled: Boolean)
    suspend fun setDanmakuSpeed(speed: Float)
    suspend fun setDanmakuSize(size: Float)
}
