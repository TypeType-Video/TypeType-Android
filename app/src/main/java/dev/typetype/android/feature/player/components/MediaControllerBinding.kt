package dev.typetype.android.feature.player.components

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dev.typetype.android.services.PlaybackService

@OptIn(markerClass = [UnstableApi::class])
@Composable
fun rememberMediaController(): State<MediaController?> {
    val context: Context = LocalContext.current
    val controllerState = remember { mutableStateOf<MediaController?>(null) }

    DisposableEffect(context) {
        val token = SessionToken(
            context.applicationContext,
            ComponentName(context.applicationContext, PlaybackService::class.java),
        )
        val future: ListenableFuture<MediaController> =
            MediaController.Builder(context.applicationContext, token).buildAsync()
        future.addListener(
            {
                if (!future.isCancelled) {
                    runCatching { future.get() }
                        .onSuccess { controllerState.value = it }
                }
            },
            MoreExecutors.directExecutor(),
        )

        onDispose {
            controllerState.value?.release()
            controllerState.value = null
            if (!future.isDone) future.cancel(true)
        }
    }
    return controllerState
}
