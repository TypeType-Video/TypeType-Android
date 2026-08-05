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
    fun enter(
        activity: Activity,
        aspectRatio: Rational,
        isPlaying: Boolean,
        audioOnlyAvailable: Boolean,
        sourceRect: Rect?,
    ) {
        activity.rememberAudioOnlyAvailability(audioOnlyAvailable)
        val params = buildParams(
            activity,
            aspectRatio,
            false,
            isPlaying,
            audioOnlyAvailable,
            sourceRect,
        )
        runCatching { activity.enterPictureInPictureMode(params) }
    }

    fun apply(
        activity: Activity,
        aspectRatio: Rational,
        autoEnter: Boolean,
        isPlaying: Boolean,
        audioOnlyAvailable: Boolean,
        sourceRect: Rect?,
    ) {
        activity.rememberAudioOnlyAvailability(audioOnlyAvailable)
        val params = buildParams(
            activity,
            aspectRatio,
            autoEnter,
            isPlaying,
            audioOnlyAvailable,
            sourceRect,
        )
        runCatching { activity.setPictureInPictureParams(params) }
    }

    fun updatePlaybackAction(
        activity: Activity,
        isPlaying: Boolean,
        audioOnlyAvailable: Boolean,
    ) {
        val params = PictureInPictureParams.Builder()
            .setActions(buildActions(activity, isPlaying, audioOnlyAvailable))
            .build()
        runCatching { activity.setPictureInPictureParams(params) }
    }

    internal fun buildParams(
        activity: Activity,
        aspectRatio: Rational,
        autoEnter: Boolean,
        isPlaying: Boolean,
        audioOnlyAvailable: Boolean,
        sourceRect: Rect?,
    ): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .setActions(buildActions(activity, isPlaying, audioOnlyAvailable))
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

    private fun buildActions(
        activity: Activity,
        isPlaying: Boolean,
        audioOnlyAvailable: Boolean,
    ): List<RemoteAction> = buildList {
        add(
            buildAction(
                activity,
                PIP_ACTION_PLAY_PAUSE,
                PIP_REQUEST_PLAY_PAUSE,
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (isPlaying) R.string.player_action_pause else R.string.player_action_play,
            ),
        )
        if (audioOnlyAvailable) {
            add(
                buildAction(
                    activity,
                    PIP_ACTION_AUDIO_ONLY,
                    PIP_REQUEST_AUDIO_ONLY,
                    R.drawable.ic_headphones,
                    R.string.player_audio_only,
                ),
            )
        }
    }

    private fun buildAction(
        activity: Activity,
        action: String,
        requestCode: Int,
        icon: Int,
        title: Int,
    ): RemoteAction {
        val intent = Intent(activity, PictureInPictureActionReceiver::class.java)
            .setAction(action)
        val pending = PendingIntent.getBroadcast(
            activity,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val label = activity.getString(title)
        return RemoteAction(Icon.createWithResource(activity, icon), label, label, pending)
    }

    private fun Activity.rememberAudioOnlyAvailability(available: Boolean) {
        (this as? PictureInPictureActionStateOwner)
            ?.setPictureInPictureAudioOnlyAvailable(available)
    }
}
