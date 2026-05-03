package dev.typetype.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.typetype.android.core.ui.theme.TypeTypeTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { viewModel.state.value.isLoading }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TypeTypeTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val startRoute = state.startRoute
                if (startRoute != null) {
                    AppNavHost(startRoute = startRoute)
                }
            }
        }
    }
}
