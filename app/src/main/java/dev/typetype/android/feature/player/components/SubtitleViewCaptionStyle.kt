package dev.typetype.android.feature.player.components

import android.graphics.Typeface
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import dev.typetype.android.domain.usersettings.CaptionStyles

@UnstableApi
internal fun SubtitleView.applyCaptionStyle(styles: CaptionStyles) {
    val spec = styles.toCaptionStyleSpec()
    setApplyEmbeddedStyles(false)
    setApplyEmbeddedFontSizes(false)
    setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * spec.textScale)
    setStyle(spec.toCaptionStyleCompat())
}

@UnstableApi
internal fun CaptionStyleSpec.toCaptionStyleCompat() = CaptionStyleCompat(
    foregroundColor,
    backgroundColor,
    windowColor,
    edge.toMedia3EdgeType(),
    edge.edgeColor(),
    Typeface.create(fontFamily.androidFamilyName(), Typeface.NORMAL),
)

private fun CaptionFontFamily.androidFamilyName(): String = when (this) {
    CaptionFontFamily.MonospaceSerif -> "serif-monospace"
    CaptionFontFamily.MonospaceSans -> "sans-serif-monospace"
    CaptionFontFamily.ProportionalSans -> "sans-serif"
    CaptionFontFamily.Casual -> "cursive"
    CaptionFontFamily.Cursive -> "cursive"
    CaptionFontFamily.SmallCapitals -> "sans-serif-smallcaps"
    CaptionFontFamily.Serif -> "serif"
}

@UnstableApi
private fun CaptionEdge.toMedia3EdgeType(): Int = when (this) {
    CaptionEdge.None -> CaptionStyleCompat.EDGE_TYPE_NONE
    CaptionEdge.DropShadow -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
    CaptionEdge.Raised -> CaptionStyleCompat.EDGE_TYPE_RAISED
    CaptionEdge.Depressed -> CaptionStyleCompat.EDGE_TYPE_DEPRESSED
    CaptionEdge.Outline -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
}

private fun CaptionEdge.edgeColor(): Int = when (this) {
    CaptionEdge.Depressed -> DEPRESSED_EDGE_COLOR
    else -> DARK_EDGE_COLOR
}

private const val DARK_EDGE_COLOR = 0xFF222222.toInt()
private const val DEPRESSED_EDGE_COLOR = 0xFFCCCCCC.toInt()
