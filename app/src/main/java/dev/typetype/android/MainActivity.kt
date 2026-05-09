package dev.typetype.android

import android.content.BroadcastReceiver
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
import dagger.hilt.android.AndroidEntryPoint
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.feature.player.components.PIP_ACTION_AUDIO_ONLY

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == PIP_ACTION_AUDIO_ONLY) {
                viewModel.playerHostController.minimize()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    isInPictureInPictureMode
                ) {
                    runCatching { moveTaskToBack(false) }
                }
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
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(PIP_ACTION_AUDIO_ONLY)
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
}
