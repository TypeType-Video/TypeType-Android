package dev.typetype.android.feature.player.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PictureInPictureActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = when (intent.action) {
            PIP_ACTION_AUDIO_ONLY -> PIP_ACTION_AUDIO_ONLY
            PIP_ACTION_PLAY_PAUSE -> PIP_ACTION_PLAY_PAUSE
            else -> return
        }
        context.sendBroadcast(Intent(action).setPackage(context.packageName))
    }
}
