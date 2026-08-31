package video.typetype.tv.ui.theme

import androidx.compose.runtime.Immutable

public enum class TvPersonality {
    Classic,
    Manga,
}

public enum class TvColorTheme {
    TypeType,
    Dynamic,
    Nord,
    Cream,
    Forest,
    Plum,
}

public enum class TvColorMode {
    System,
    Light,
    Dark,
}

public enum class TvMangaPaper {
    Day,
    Night,
    Nord,
}

public enum class TvHeadlineMarker {
    None,
    Stamp,
    SpeedLines,
}

public enum class TvMotion {
    Full,
    Subtle,
    Off,
}

@Immutable
public data class TvAppearance(
    val personality: TvPersonality = TvPersonality.Classic,
    val colorTheme: TvColorTheme = TvColorTheme.TypeType,
    val colorMode: TvColorMode = TvColorMode.Dark,
    val amoled: Boolean = false,
    val mangaPaper: TvMangaPaper = TvMangaPaper.Day,
    val headlineMarker: TvHeadlineMarker = TvHeadlineMarker.Stamp,
    val screentone: Boolean = true,
    val speedLines: Boolean = true,
    val starburst: Boolean = true,
    val inkedIcons: Boolean = true,
    val panelTilt: Boolean = false,
    val motion: TvMotion = TvMotion.Subtle,
) {
    public val isManga: Boolean get() = personality == TvPersonality.Manga

    public val transitionMillis: Int
        get() = when (motion) {
            TvMotion.Full -> 360
            TvMotion.Subtle -> 180
            TvMotion.Off -> 0
        }
}
