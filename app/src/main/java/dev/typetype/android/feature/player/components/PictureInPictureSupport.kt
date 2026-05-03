package dev.typetype.android.feature.player.components

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.util.Rational
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun rememberIsInPipMode(): Boolean {
    val configuration = LocalConfiguration.current
    val activity = LocalActivity.current ?: return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        activity.isInPictureInPictureMode
    } else {
        configuration.uiMode == Configuration.UI_MODE_TYPE_NORMAL && false
    }
}

fun enterPictureInPicture(activity: Activity?, aspectRatio: Rational = Rational(16, 9)) {
    if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val params = PictureInPictureParams.Builder()
        .setAspectRatio(aspectRatio)
        .build()
    runCatching { activity.enterPictureInPictureMode(params) }
}
