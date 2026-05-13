package dev.typetype.android

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import dev.typetype.android.core.ui.components.FullScreenLoader
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.feature.player.components.PIP_ACTION_AUDIO_ONLY
import dev.typetype.android.feature.player.components.PIP_ACTION_FORWARD
import dev.typetype.android.feature.player.components.PIP_ACTION_PLAY_PAUSE
import dev.typetype.android.feature.player.components.PIP_ACTION_REWIND

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                PIP_ACTION_AUDIO_ONLY -> enterAudioOnlyMode()
                PIP_ACTION_REWIND,
                PIP_ACTION_PLAY_PAUSE,
                PIP_ACTION_FORWARD,
                -> dispatchPipPlayerAction(intent.action)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { viewModel.state.value.isLoading }
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            }
        }
        super.onCreate(savedInstanceState)
        setContent {
            val preferences by viewModel.preferences.collectAsStateWithLifecycle()
            TypeTypeTheme(accentColor = preferences.accentColor) {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val startRoute = state.startRoute
                if (startRoute != null) {
                    AppNavHost(startRoute = startRoute, mainViewModel = viewModel)
                } else {
                    FullScreenLoader()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(PIP_ACTION_AUDIO_ONLY)
            addAction(PIP_ACTION_REWIND)
            addAction(PIP_ACTION_PLAY_PAUSE)
            addAction(PIP_ACTION_FORWARD)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pipReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(pipReceiver, filter)
        }
    }

    override fun onStop() {
        runCatching { unregisterReceiver(pipReceiver) }
        super.onStop()
    }

    private fun enterAudioOnlyMode() {
        viewModel.playerHostController.minimize()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode) {
            runCatching { moveTaskToBack(false) }
        }
    }

    private fun dispatchPipPlayerAction(action: String?) {
        val token = SessionToken(
            applicationContext,
            ComponentName(applicationContext, "dev.typetype.android.services.PlaybackService"),
        )
        val future = MediaController.Builder(applicationContext, token).buildAsync()
        future.addListener(
            {
                val controller = runCatching { future.get() }.getOrNull() ?: return@addListener
                runCatching {
                    when (action) {
                        PIP_ACTION_REWIND -> controller.seekBack()
                        PIP_ACTION_PLAY_PAUSE -> {
                            if (controller.isPlaying) {
                                controller.pause()
                            } else {
                                controller.play()
                            }
                        }
                        PIP_ACTION_FORWARD -> controller.seekForward()
                    }
                }
                controller.release()
            },
            MoreExecutors.directExecutor(),
        )
    }
}
