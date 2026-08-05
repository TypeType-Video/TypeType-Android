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
    val refreshGate = remember(streamId) { PlayerSurfaceRefreshGate() }
    DisposableEffect(lifecycleOwner, streamId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> refreshGate.invalidate()
                Lifecycle.Event.ON_START -> if (refreshGate.refresh()) epoch += 1
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(context, lifecycleOwner, streamId) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF -> refreshGate.invalidate()
                    Intent.ACTION_SCREEN_ON,
                    Intent.ACTION_USER_PRESENT,
                    -> if (
                        lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) &&
                        refreshGate.refresh()
                    ) {
                        epoch += 1
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
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

internal class PlayerSurfaceRefreshGate {
    private var invalid = false

    fun invalidate() {
        invalid = true
    }

    fun refresh(): Boolean {
        if (!invalid) return false
        invalid = false
        return true
    }
}
