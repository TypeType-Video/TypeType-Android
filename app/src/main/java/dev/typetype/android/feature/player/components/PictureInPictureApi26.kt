package dev.typetype.android.feature.player.components

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import dev.typetype.android.R

@RequiresApi(Build.VERSION_CODES.O)
internal object PictureInPictureApi26 {
    fun enter(activity: Activity, aspectRatio: Rational, isPlaying: Boolean, sourceRect: Rect?) {
        val params = buildParams(activity, aspectRatio, false, isPlaying, sourceRect)
        runCatching { activity.enterPictureInPictureMode(params) }
    }

    fun apply(
        activity: Activity,
        aspectRatio: Rational,
        autoEnter: Boolean,
        isPlaying: Boolean,
        sourceRect: Rect?,
    ) {
        val params = buildParams(activity, aspectRatio, autoEnter, isPlaying, sourceRect)
        runCatching { activity.setPictureInPictureParams(params) }
    }

    private fun buildParams(
        activity: Activity,
        aspectRatio: Rational,
        autoEnter: Boolean,
        isPlaying: Boolean,
        sourceRect: Rect?,
    ): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .setActions(buildActions(activity, isPlaying))
        sourceRect?.takeIf { it.width() > 0 && it.height() > 0 }?.let {
            builder.setSourceRectHint(Rect(it))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter)
                .setSeamlessResizeEnabled(true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            builder.setTitle(activity.getString(R.string.app_name))
                .setSubtitle(
                    activity.getString(
                        if (isPlaying) R.string.player_state_playing
                        else R.string.player_state_paused,
                    ),
                )
        }
        return builder.build()
    }

    private fun buildActions(activity: Activity, isPlaying: Boolean): List<RemoteAction> {
        return listOf(
            buildAction(activity, PIP_ACTION_REWIND, PIP_REQUEST_REWIND, R.drawable.ic_rewind, R.string.player_rewind),
            buildAction(
                activity,
                PIP_ACTION_PLAY_PAUSE,
                PIP_REQUEST_PLAY_PAUSE,
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (isPlaying) R.string.player_action_pause else R.string.player_action_play,
            ),
            buildAction(activity, PIP_ACTION_FORWARD, PIP_REQUEST_FORWARD, R.drawable.ic_forward, R.string.player_forward),
            buildAction(
                activity,
                PIP_ACTION_AUDIO_ONLY,
                PIP_REQUEST_AUDIO_ONLY,
                R.drawable.ic_headphones,
                R.string.player_audio_only,
            ),
        )
    }

    private fun buildAction(
        activity: Activity,
        action: String,
        requestCode: Int,
        icon: Int,
        title: Int,
    ): RemoteAction {
        val intent = Intent(action).setPackage(activity.packageName)
        val pending = PendingIntent.getBroadcast(
            activity,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val label = activity.getString(title)
        return RemoteAction(Icon.createWithResource(activity, icon), label, label, pending)
    }
}
