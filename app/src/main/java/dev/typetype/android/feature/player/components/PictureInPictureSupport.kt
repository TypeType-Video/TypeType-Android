package dev.typetype.android.feature.player.components

import android.app.Activity
import android.app.PictureInPictureParams
import android.util.Rational
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable

private val DEFAULT_ASPECT_RATIO = Rational(16, 9)

@Composable
fun rememberIsInPipMode(): Boolean {
    val activity = LocalActivity.current ?: return false
    return activity.isInPictureInPictureMode
}

fun enterPictureInPicture(activity: Activity?, aspectRatio: Rational = DEFAULT_ASPECT_RATIO) {
    activity ?: return
    val params = PictureInPictureParams.Builder()
        .setAspectRatio(aspectRatio)
        .build()
    runCatching { activity.enterPictureInPictureMode(params) }
}

fun applyAutoEnterPipParams(
    activity: Activity?,
    autoEnter: Boolean,
    aspectRatio: Rational = DEFAULT_ASPECT_RATIO,
) {
    activity ?: return
    val params = PictureInPictureParams.Builder()
        .setAspectRatio(aspectRatio)
        .setAutoEnterEnabled(autoEnter)
        .build()
    runCatching { activity.setPictureInPictureParams(params) }
}
