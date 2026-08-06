package dev.typetype.android.feature.player.components

import android.graphics.Typeface
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import dev.typetype.android.domain.usersettings.CaptionStyles
import org.junit.Assert.assertEquals
import org.junit.Test

@UnstableApi
class CaptionStyleCompatTest {
    @Test
    fun media3StyleUsesResolvedServerSettings() {
        val style = CaptionStyles(
            fontFamily = "mono-sans",
            textColor = "#ffff00",
            textOpacity = "75%",
            textShadow = "drop shadow",
            textBackgroundOpacity = "50%",
            displayBackgroundOpacity = "25%",
        ).toCaptionStyleSpec().toCaptionStyleCompat()

        assertEquals(0xBFFFFF00.toInt(), style.foregroundColor)
        assertEquals(0x80000000.toInt(), style.backgroundColor)
        assertEquals(0x40000000, style.windowColor)
        assertEquals(CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW, style.edgeType)
        assertEquals(0xFF222222.toInt(), style.edgeColor)
        assertEquals(Typeface.create("sans-serif-monospace", Typeface.NORMAL), style.typeface)
    }

    @Test
    fun monoSerifKeepsASeparateAndroidTypeface() {
        val style = CaptionStyles(fontFamily = "mono-serif")
            .toCaptionStyleSpec()
            .toCaptionStyleCompat()

        assertEquals(Typeface.create("serif-monospace", Typeface.NORMAL), style.typeface)
    }
}
