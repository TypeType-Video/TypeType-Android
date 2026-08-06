package dev.typetype.android.feature.player.components

import dev.typetype.android.domain.usersettings.CaptionStyles
import org.junit.Assert.assertEquals
import org.junit.Test

class CaptionStyleSpecTest {
    @Test
    fun emptySettingsResolveToFrontendDefaults() {
        val spec = CaptionStyles().toCaptionStyleSpec()

        assertEquals(CaptionFontFamily.ProportionalSans, spec.fontFamily)
        assertEquals(1f, spec.textScale)
        assertEquals(0xFFFFFFFF.toInt(), spec.foregroundColor)
        assertEquals(0xFF000000.toInt(), spec.backgroundColor)
        assertEquals(0x00000000, spec.windowColor)
        assertEquals(CaptionEdge.None, spec.edge)
    }

    @Test
    fun customSettingsPreserveColorOpacityFamilySizeAndEdge() {
        val spec = CaptionStyles(
            fontFamily = "mono-serif",
            fontSize = "175%",
            textColor = "#0f8",
            textOpacity = "50%",
            textShadow = "outline",
            textBackground = "#123456",
            textBackgroundOpacity = "25%",
            displayBackground = "#abcdef",
            displayBackgroundOpacity = "75%",
        ).toCaptionStyleSpec()

        assertEquals(CaptionFontFamily.MonospaceSerif, spec.fontFamily)
        assertEquals(1.75f, spec.textScale)
        assertEquals(0x8000FF88.toInt(), spec.foregroundColor)
        assertEquals(0x40123456, spec.backgroundColor)
        assertEquals(0xBFABCDEF.toInt(), spec.windowColor)
        assertEquals(CaptionEdge.Outline, spec.edge)
    }

    @Test
    fun invalidValuesFallbackAndBoundsRemainSafe() {
        val spec = CaptionStyles(
            fontFamily = "unexpected",
            fontSize = "900%",
            textColor = "not-a-color",
            textOpacity = "broken",
            textShadow = "unknown",
            textBackgroundOpacity = "-10%",
            displayBackgroundOpacity = "999%",
        ).toCaptionStyleSpec()

        assertEquals(CaptionFontFamily.Serif, spec.fontFamily)
        assertEquals(2f, spec.textScale)
        assertEquals(0xFFFFFFFF.toInt(), spec.foregroundColor)
        assertEquals(0x00000000, spec.backgroundColor)
        assertEquals(0xFF000000.toInt(), spec.windowColor)
        assertEquals(CaptionEdge.None, spec.edge)
    }
}
