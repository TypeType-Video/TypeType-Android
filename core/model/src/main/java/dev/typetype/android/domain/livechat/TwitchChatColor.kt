package dev.typetype.android.domain.livechat

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

// The web chat's rule: a viewer without a color gets one from their login, and a color too dark for a dark theme (or
// too light for a light one) is mixed toward white (or black) until it reads. Returns an opaque ARGB value.
object TwitchChatColor {
    private val DEFAULTS = listOf(
        "#FF0000", "#0000FF", "#008000", "#B22222", "#FF7F50", "#9ACD32", "#FF4500", "#2E8B57",
        "#DAA520", "#D2691E", "#5F9EA0", "#1E90FF", "#FF69B4", "#8A2BE2", "#00FF7F",
    )

    private val FALLBACK = intArrayOf(0x91, 0x46, 0xFF)

    fun name(color: String?, login: String, dark: Boolean): Int {
        val rgb = rgb(color?.takeIf { it.isNotBlank() } ?: DEFAULTS[hash(login)]) ?: FALLBACK
        val lum = luminance(rgb)
        val mixed = when {
            dark && lum < 0.18 -> mix(rgb, 255, min(0.75, (0.18 - lum) * 4 + 0.25))
            !dark && lum > 0.45 -> mix(rgb, 0, min(0.6, (lum - 0.45) * 2 + 0.2))
            else -> rgb
        }
        return (0xFF shl 24) or (mixed[0] shl 16) or (mixed[1] shl 8) or mixed[2]
    }

    private fun hash(text: String): Int {
        var value = 0
        text.codePoints().forEach { value = value * 31 + it }
        return abs(value % DEFAULTS.size)
    }

    private fun rgb(hex: String): IntArray? {
        val digits = hex.removePrefix("#")
        if (digits.length != 6) return null
        val n = digits.toIntOrNull(16) ?: return null
        return intArrayOf((n shr 16) and 255, (n shr 8) and 255, n and 255)
    }

    private fun luminance(rgb: IntArray): Double {
        fun channel(v: Int): Double {
            val c = v / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(rgb[0]) + 0.7152 * channel(rgb[1]) + 0.0722 * channel(rgb[2])
    }

    private fun mix(rgb: IntArray, target: Int, amount: Double): IntArray =
        IntArray(3) { (rgb[it] + (target - rgb[it]) * amount).roundToInt() }
}
