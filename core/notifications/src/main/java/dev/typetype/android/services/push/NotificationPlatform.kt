package dev.typetype.android.services.push

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import dev.typetype.android.R

internal enum class NotificationPlatform(
    val serviceId: Int,
    @param:StringRes val nameRes: Int,
    @param:DrawableRes val iconRes: Int,
    @param:ColorInt val color: Int,
) {
    YouTube(0, R.string.settings_default_service_youtube, R.drawable.ic_service_youtube, 0xFFFF0000.toInt()),
    BiliBili(5, R.string.settings_default_service_bilibili, R.drawable.ic_service_bilibili, 0xFF00A1D6.toInt()),
    NicoNico(6, R.string.settings_default_service_niconico, R.drawable.ic_service_niconico, 0xFF231815.toInt()),
    ;

    companion object {
        fun of(serviceId: Int): NotificationPlatform? = entries.firstOrNull { it.serviceId == serviceId }
    }
}

internal fun NotificationPlatform.badge(context: Context): Bitmap? {
    val glyph = ContextCompat.getDrawable(context, iconRes)?.mutate() ?: return null
    val size = context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = this@badge.color }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    val inset = size / 4
    glyph.setTint(Color.WHITE)
    glyph.setBounds(inset, inset, size - inset, size - inset)
    glyph.draw(canvas)
    return bitmap
}

internal fun Bitmap.withPlatformBadge(context: Context, platform: NotificationPlatform?): Bitmap {
    val size = context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
    val side = minOf(width, height)
    val icon = createBitmap(size, size)
    val canvas = Canvas(icon)
    val crop = Rect((width - side) / 2, (height - side) / 2, (width + side) / 2, (height + side) / 2)
    canvas.drawBitmap(this, crop, Rect(0, 0, size, size), Paint(Paint.FILTER_BITMAP_FLAG))
    val badge = platform?.badge(context) ?: return icon
    val badgeSize = size * 2 / 5
    val start = (size - badgeSize).toFloat()
    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    canvas.drawCircle(start + badgeSize / 2f, start + badgeSize / 2f, badgeSize / 2f + size / 48f, ring)
    canvas.drawBitmap(badge, null, RectF(start, start, size.toFloat(), size.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
    return icon
}
