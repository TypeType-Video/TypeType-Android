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
const val PIP_ACTION_REWIND = "dev.typetype.android.PIP_REWIND"
const val PIP_ACTION_PLAY_PAUSE = "dev.typetype.android.PIP_PLAY_PAUSE"
const val PIP_ACTION_FORWARD = "dev.typetype.android.PIP_FORWARD"
private const val PIP_REQUEST_AUDIO_ONLY = 1010
private const val PIP_REQUEST_REWIND = 1011
private const val PIP_REQUEST_PLAY_PAUSE = 1012
private const val PIP_REQUEST_FORWARD = 1013

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
    isPlaying: Boolean = false,
) {
    activity ?: return
    val params = buildParams(activity, aspectRatio, isPlaying = isPlaying)
    runCatching { activity.enterPictureInPictureMode(params) }
}

fun applyAutoEnterPipParams(
    activity: Activity?,
    autoEnter: Boolean,
    aspectRatio: Rational = DEFAULT_ASPECT_RATIO,
    isPlaying: Boolean = false,
) {
    activity ?: return
    val params = buildParams(activity, aspectRatio, autoEnter = autoEnter, isPlaying = isPlaying)
    runCatching { activity.setPictureInPictureParams(params) }
}

private fun buildParams(
    activity: Activity,
    aspectRatio: Rational,
    autoEnter: Boolean = false,
    isPlaying: Boolean = false,
): PictureInPictureParams {
    val builder = PictureInPictureParams.Builder()
        .setAspectRatio(aspectRatio)
        .setActions(
            listOf(
                buildAction(
                    activity = activity,
                    action = PIP_ACTION_REWIND,
                    requestCode = PIP_REQUEST_REWIND,
                    icon = R.drawable.ic_rewind,
                    title = R.string.player_rewind,
                ),
                buildAction(
                    activity = activity,
                    action = PIP_ACTION_PLAY_PAUSE,
                    requestCode = PIP_REQUEST_PLAY_PAUSE,
                    icon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                    title = R.string.player_play_pause,
                ),
                buildAction(
                    activity = activity,
                    action = PIP_ACTION_FORWARD,
                    requestCode = PIP_REQUEST_FORWARD,
                    icon = R.drawable.ic_forward,
                    title = R.string.player_forward,
                ),
                buildAction(
                    activity = activity,
                    action = PIP_ACTION_AUDIO_ONLY,
                    requestCode = PIP_REQUEST_AUDIO_ONLY,
                    icon = R.drawable.ic_headphones,
                    title = R.string.player_audio_only,
                ),
            ),
        )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        builder.setAutoEnterEnabled(autoEnter)
            .setSeamlessResizeEnabled(true)
    }
    return builder.build()
}

private fun buildAction(
    activity: Activity,
    action: String,
    requestCode: Int,
    icon: Int,
    title: Int,
): android.app.RemoteAction {
    val intent = Intent(action).setPackage(activity.packageName)
    val pending = PendingIntent.getBroadcast(
        activity,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val actionIcon = Icon.createWithResource(activity, icon)
    val label = activity.getString(title)
    return android.app.RemoteAction(actionIcon, label, label, pending)
}
