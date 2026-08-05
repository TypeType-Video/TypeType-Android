package dev.typetype.android.feature.player.components

import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer

private val DEFAULT_ASPECT_RATIO = Rational(16, 9)

const val PIP_ACTION_AUDIO_ONLY = "dev.typetype.android.PIP_AUDIO_ONLY"
const val PIP_ACTION_PLAY_PAUSE = "dev.typetype.android.PIP_PLAY_PAUSE"
internal const val PIP_REQUEST_AUDIO_ONLY = 1010
internal const val PIP_REQUEST_PLAY_PAUSE = 1011

@Composable
fun rememberIsInPipMode(): State<Boolean> {
    val activity = LocalActivity.current as? ComponentActivity
    val state = remember(activity) {
        mutableStateOf(isInPictureInPictureMode(activity))
    }
    DisposableEffect(activity) {
        if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
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
    audioOnlyAvailable: Boolean = false,
    sourceRect: Rect? = null,
) {
    val host = activity ?: return
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    if (!supportsPictureInPicture(host)) return
    PictureInPictureApi26.enter(
        host,
        aspectRatio,
        isPlaying,
        audioOnlyAvailable,
        sourceRect,
    )
}

fun applyAutoEnterPipParams(
    activity: Activity?,
    autoEnter: Boolean,
    aspectRatio: Rational = DEFAULT_ASPECT_RATIO,
    isPlaying: Boolean = false,
    audioOnlyAvailable: Boolean = false,
    sourceRect: Rect? = null,
) {
    val host = activity ?: return
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    if (!supportsPictureInPicture(host)) return
    PictureInPictureApi26.apply(
        host,
        aspectRatio,
        autoEnter,
        isPlaying,
        audioOnlyAvailable,
        sourceRect,
    )
}

fun updatePictureInPicturePlaybackAction(
    activity: Activity?,
    isPlaying: Boolean,
    audioOnlyAvailable: Boolean,
) {
    val host = activity ?: return
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    PictureInPictureApi26.updatePlaybackAction(host, isPlaying, audioOnlyAvailable)
}

fun supportsPictureInPicture(activity: Activity?): Boolean {
    return activity != null &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
}

private fun isInPictureInPictureMode(activity: Activity?): Boolean {
    return activity != null &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
        activity.isInPictureInPictureMode
}
