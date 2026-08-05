package dev.typetype.android

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import dev.typetype.android.core.ui.components.FullScreenLoader
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.auth.OidcCallbackRelay
import dev.typetype.android.domain.auth.OidcRedirect
import dev.typetype.android.domain.navigation.resolveIncomingVideoUrl
import dev.typetype.android.domain.navigation.resolveSharedVideoUrl
import dev.typetype.android.domain.session.ActiveSessionRepository
import dev.typetype.android.feature.player.components.PIP_ACTION_AUDIO_ONLY
import dev.typetype.android.feature.player.components.PIP_ACTION_PLAY_PAUSE
import dev.typetype.android.feature.player.components.PictureInPictureActionStateOwner
import dev.typetype.android.feature.player.components.updatePictureInPicturePlaybackAction
import dev.typetype.android.feature.settings.diagnostics.CrashReportRoute
import dev.typetype.android.services.PlaybackAudioOnlyCommand
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity(), PictureInPictureActionStateOwner {

    @Inject
    lateinit var activeSessionRepository: ActiveSessionRepository

    @Inject
    lateinit var oidcCallbackRelay: OidcCallbackRelay

    private val viewModel: MainViewModel by viewModels()
    private var activityReportingJob: Job? = null
    private var pipAudioOnlyAvailable = false

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                PIP_ACTION_AUDIO_ONLY -> enterAudioOnlyMode()
                PIP_ACTION_PLAY_PAUSE -> togglePipPlayback()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition {
            val state = viewModel.state.value
            state.isLoading && state.pendingCrashReport == null
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            }
        }
        super.onCreate(savedInstanceState)
        receiveOidcCallback(intent)
        if (savedInstanceState == null) receiveExternalVideo(intent)
        registerPipReceiver()
        setContent {
            val preferences by viewModel.preferences.collectAsStateWithLifecycle()
            TypeTypeTheme(accentColor = preferences.accentColor) {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val startRoute = state.startRoute
                val pendingCrashReport = state.pendingCrashReport
                when {
                    pendingCrashReport != null -> CrashReportRoute(
                        report = pendingCrashReport,
                        onContinue = viewModel::continueAfterCrash,
                    )
                    startRoute != null -> AppNavHost(startRoute = startRoute, mainViewModel = viewModel)
                    else -> FullScreenLoader()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activityReportingJob?.cancel()
        activityReportingJob = lifecycleScope.launch {
            while (isActive) {
                activeSessionRepository.reportActivity()
                delay(ACTIVITY_REPORT_INTERVAL_MILLIS)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveOidcCallback(intent)
        receiveExternalVideo(intent)
    }

    override fun onStop() {
        activityReportingJob?.cancel()
        activityReportingJob = null
        super.onStop()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(pipReceiver) }
        super.onDestroy()
    }

    override fun setPictureInPictureAudioOnlyAvailable(available: Boolean) {
        pipAudioOnlyAvailable = available
    }

    private fun registerPipReceiver() {
        val filter = IntentFilter().apply {
            addAction(PIP_ACTION_AUDIO_ONLY)
            addAction(PIP_ACTION_PLAY_PAUSE)
        }
        ContextCompat.registerReceiver(
            this,
            pipReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    private fun receiveOidcCallback(intent: Intent) {
        val callbackUrl = intent.dataString ?: return
        if (OidcRedirect.matches(callbackUrl)) {
            oidcCallbackRelay.submit(callbackUrl)
        }
    }

    private fun receiveExternalVideo(intent: Intent) {
        val videoUrl = when (intent.action) {
            Intent.ACTION_VIEW -> resolveIncomingVideoUrl(intent.dataString)
            Intent.ACTION_SEND -> intent.takeIf { it.type?.startsWith("text/") == true }
                ?.getStringExtra(Intent.EXTRA_TEXT)
                ?.let(::resolveSharedVideoUrl)
            else -> null
        } ?: return
        viewModel.openExternalVideo(videoUrl)
        setIntent(
            Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
            },
        )
    }

    private fun enterAudioOnlyMode() {
        withPlaybackController { controller ->
            val commandFuture = controller.sendCustomCommand(
                PlaybackAudioOnlyCommand.command,
                PlaybackAudioOnlyCommand.arguments(true),
            )
            commandFuture.addListener(
                {
                    val succeeded = runCatching { commandFuture.get() }
                        .getOrNull()
                        ?.resultCode == SessionResult.RESULT_SUCCESS
                    if (succeeded) {
                        runOnUiThread {
                            viewModel.playerHostController.minimize()
                            if (
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                                isInPictureInPictureMode
                            ) {
                                runCatching { moveTaskToBack(false) }
                            }
                        }
                    }
                    controller.release()
                },
                MoreExecutors.directExecutor(),
            )
        }
    }

    private fun togglePipPlayback() {
        withPlaybackController { controller ->
            val shouldPlay = !controller.playWhenReady
            if (shouldPlay) controller.play() else controller.pause()
            runOnUiThread {
                updatePictureInPicturePlaybackAction(
                    this,
                    shouldPlay,
                    pipAudioOnlyAvailable,
                )
            }
            controller.release()
        }
    }

    private fun withPlaybackController(onConnected: (MediaController) -> Unit) {
        val token = SessionToken(
            applicationContext,
            ComponentName(applicationContext, "dev.typetype.android.services.PlaybackService"),
        )
        val controllerFuture = MediaController.Builder(applicationContext, token).buildAsync()
        controllerFuture.addListener(
            {
                val controller = runCatching { controllerFuture.get() }.getOrNull()
                    ?: return@addListener
                onConnected(controller)
            },
            MoreExecutors.directExecutor(),
        )
    }

    private companion object {
        const val ACTIVITY_REPORT_INTERVAL_MILLIS = 60_000L
    }
}
