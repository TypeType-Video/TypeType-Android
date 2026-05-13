package dev.typetype.android.feature.player.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
internal fun rememberPlayerSurfaceKey(streamId: String): String {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var epoch by remember(streamId) { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner, streamId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
                epoch += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(context, streamId) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                epoch += 1
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(
            context.applicationContext,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.applicationContext.unregisterReceiver(receiver) }
    }
    return "$streamId:$epoch"
}
