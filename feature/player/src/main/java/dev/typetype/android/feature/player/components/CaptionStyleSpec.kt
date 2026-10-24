package dev.typetype.android.feature.player.components

import dev.typetype.android.domain.usersettings.CaptionStyles
import dev.typetype.android.domain.usersettings.resolvedDisplayBackground
import dev.typetype.android.domain.usersettings.resolvedDisplayBackgroundOpacity
import dev.typetype.android.domain.usersettings.resolvedFontFamily
import dev.typetype.android.domain.usersettings.resolvedFontSize
import dev.typetype.android.domain.usersettings.resolvedTextBackground
import dev.typetype.android.domain.usersettings.resolvedTextBackgroundOpacity
import dev.typetype.android.domain.usersettings.resolvedTextColor
import dev.typetype.android.domain.usersettings.resolvedTextOpacity
import dev.typetype.android.domain.usersettings.resolvedTextShadow

internal data class CaptionStyleSpec(
    val fontFamily: CaptionFontFamily,
    val textScale: Float,
    val foregroundColor: Int,
    val backgroundColor: Int,
    val windowColor: Int,
    val edge: CaptionEdge,
)

internal enum class CaptionFontFamily {
    MonospaceSerif,
    MonospaceSans,
    ProportionalSans,
    Casual,
    Cursive,
    SmallCapitals,
    Serif,
}

internal enum class CaptionEdge {
    None,
    DropShadow,
    Raised,
    Depressed,
    Outline,
}

internal fun CaptionStyles.toCaptionStyleSpec(): CaptionStyleSpec = CaptionStyleSpec(
    fontFamily = resolvedFontFamily().toCaptionFontFamily(),
    textScale = resolvedFontSize().percent(DEFAULT_SIZE_PERCENT, MIN_SIZE_PERCENT, MAX_SIZE_PERCENT) / 100f,
    foregroundColor = resolvedTextColor().withOpacity(
        resolvedTextOpacity().percent(DEFAULT_OPACITY_PERCENT),
        DEFAULT_FOREGROUND_COLOR,
    ),
    backgroundColor = resolvedTextBackground().withOpacity(
        resolvedTextBackgroundOpacity().percent(DEFAULT_OPACITY_PERCENT),
        DEFAULT_BACKGROUND_COLOR,
    ),
    windowColor = resolvedDisplayBackground().withOpacity(
        resolvedDisplayBackgroundOpacity().percent(DEFAULT_WINDOW_OPACITY_PERCENT),
        DEFAULT_BACKGROUND_COLOR,
    ),
    edge = resolvedTextShadow().toCaptionEdge(),
)

private fun String.toCaptionFontFamily(): CaptionFontFamily = when (this) {
    "mono-serif" -> CaptionFontFamily.MonospaceSerif
    "mono-sans" -> CaptionFontFamily.MonospaceSans
    "pro-sans" -> CaptionFontFamily.ProportionalSans
    "casual" -> CaptionFontFamily.Casual
    "cursive" -> CaptionFontFamily.Cursive
    "capitals" -> CaptionFontFamily.SmallCapitals
    else -> CaptionFontFamily.Serif
}

private fun String.toCaptionEdge(): CaptionEdge = when (lowercase()) {
    "drop shadow" -> CaptionEdge.DropShadow
    "raised" -> CaptionEdge.Raised
    "depressed" -> CaptionEdge.Depressed
    "outline" -> CaptionEdge.Outline
    else -> CaptionEdge.None
}

private fun String.percent(
    fallback: Int,
    minimum: Int = MIN_OPACITY_PERCENT,
    maximum: Int = MAX_OPACITY_PERCENT,
): Int = removeSuffix("%").toIntOrNull()?.coerceIn(minimum, maximum) ?: fallback

private fun String.withOpacity(opacityPercent: Int, fallbackColor: Int): Int {
    val rgb = parseRgb() ?: (fallbackColor and RGB_MASK)
    val alpha = (opacityPercent * MAX_ALPHA + HALF_PERCENT) / MAX_OPACITY_PERCENT
    return (alpha shl ALPHA_SHIFT) or rgb
}

private fun String.parseRgb(): Int? {
    val hex = removePrefix("#")
    val expanded = when (hex.length) {
        3 -> buildString(6) { hex.forEach { append(it).append(it) } }
        6 -> hex
        else -> return null
    }
    return expanded.toIntOrNull(16)?.and(RGB_MASK)
}

private const val DEFAULT_SIZE_PERCENT = 100
private const val MIN_SIZE_PERCENT = 50
private const val MAX_SIZE_PERCENT = 200
private const val DEFAULT_OPACITY_PERCENT = 100
private const val DEFAULT_WINDOW_OPACITY_PERCENT = 0
private const val MIN_OPACITY_PERCENT = 0
private const val MAX_OPACITY_PERCENT = 100
private const val MAX_ALPHA = 255
private const val HALF_PERCENT = 50
private const val ALPHA_SHIFT = 24
private const val RGB_MASK = 0x00FFFFFF
private const val DEFAULT_FOREGROUND_COLOR = 0x00FFFFFF
private const val DEFAULT_BACKGROUND_COLOR = 0x00000000
