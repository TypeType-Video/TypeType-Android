package dev.typetype.android.feature.player.components

import android.app.Activity
import android.app.PictureInPictureParams
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.util.Consumer
import dev.typetype.android.R

private val DEFAULT_ASPECT_RATIO = Rational(16, 9)

const val PIP_ACTION_AUDIO_ONLY = "dev.typetype.android.PIP_AUDIO_ONLY"
private const val PIP_REQUEST_AUDIO_ONLY = 1010

@Composable
fun rememberIsInPipMode(): State<Boolean> {
    val activity = LocalActivity.current as? ComponentActivity
    val state = remember(activity) {
        mutableStateOf(activity?.isInPictureInPictureMode == true)
    }
    DisposableEffect(activity) {
        if (activity == null) {
            onDispose { }
        } else {
            val listener = Consumer<PictureInPictureModeChangedInfo> { info ->
                state.value = info.isInPictureInPictureMode
            }
            activity.addOnPictureInPictureModeChangedListener(listener)
            onDispose { activity.removeOnPictureInPictureModeChangedListener(listener) }
        }
    }
    return state
}

fun enterPictureInPicture(
    activity: Activity?,
    aspectRatio: Rational = DEFAULT_ASPECT_RATIO,
) {
    activity ?: return
    val params = buildParams(activity, aspectRatio)
    runCatching { activity.enterPictureInPictureMode(params) }
}

fun applyAutoEnterPipParams(
    activity: Activity?,
    autoEnter: Boolean,
    aspectRatio: Rational = DEFAULT_ASPECT_RATIO,
) {
    activity ?: return
    val params = buildParams(activity, aspectRatio, autoEnter = autoEnter)
    runCatching { activity.setPictureInPictureParams(params) }
}

private fun buildParams(
    activity: Activity,
    aspectRatio: Rational,
    autoEnter: Boolean = false,
): PictureInPictureParams {
    val builder = PictureInPictureParams.Builder()
        .setAspectRatio(aspectRatio)
        .setActions(listOf(buildHeadphonesAction(activity)))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        builder.setAutoEnterEnabled(autoEnter)
            .setSeamlessResizeEnabled(true)
    }
    return builder.build()
}

private fun buildHeadphonesAction(activity: Activity): android.app.RemoteAction {
    val intent = Intent(PIP_ACTION_AUDIO_ONLY).setPackage(activity.packageName)
    val pending = PendingIntent.getBroadcast(
        activity,
        PIP_REQUEST_AUDIO_ONLY,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val icon = Icon.createWithResource(activity, R.drawable.ic_headphones)
    return android.app.RemoteAction(icon, "Audio only", "Continue audio in background", pending)
}
