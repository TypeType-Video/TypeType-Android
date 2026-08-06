package dev.typetype.android.domain.usersettings

data class CaptionStyles(
    val fontFamily: String = "",
    val fontSize: String = "",
    val textColor: String = "",
    val textOpacity: String = "",
    val textShadow: String = "",
    val textBackground: String = "",
    val textBackgroundOpacity: String = "",
    val displayBackground: String = "",
    val displayBackgroundOpacity: String = "",
) {
    companion object {
        const val DEFAULT_FONT_FAMILY = "pro-sans"
        const val DEFAULT_FONT_SIZE = "100%"
        const val DEFAULT_TEXT_COLOR = "#ffffff"
        const val DEFAULT_TEXT_OPACITY = "100%"
        const val DEFAULT_TEXT_SHADOW = "none"
        const val DEFAULT_TEXT_BACKGROUND = "#000000"
        const val DEFAULT_TEXT_BACKGROUND_OPACITY = "100%"
        const val DEFAULT_DISPLAY_BACKGROUND = "#000000"
        const val DEFAULT_DISPLAY_BACKGROUND_OPACITY = "0%"
    }
}
