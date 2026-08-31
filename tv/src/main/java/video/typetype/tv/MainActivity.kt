package video.typetype.tv

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import video.typetype.tv.data.TypeTypeTvClient
import video.typetype.tv.data.TvViewModel
import video.typetype.tv.data.TvArtifactStore
import video.typetype.tv.data.TvDownloadStateStore
import video.typetype.tv.data.handleOidcCallback
import video.typetype.tv.player.TvPlaybackCodecSupport

public class MainActivity : ComponentActivity() {
    private val viewModel: TvViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (!modelClass.isAssignableFrom(TvViewModel::class.java)) {
                    throw IllegalArgumentException("Unsupported ViewModel: ${modelClass.name}")
                }
                return requireNotNull(
                    modelClass.cast(
                        TvViewModel(
                            TypeTypeTvClient.create(this@MainActivity, BuildConfig.TYPETYPE_INSTANCE_URL),
                            TvArtifactStore(this@MainActivity),
                            TvDownloadStateStore(this@MainActivity),
                            TvPlaybackCodecSupport(this@MainActivity)::isVideoSupported,
                        ),
                    ),
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        setContent {
            TypeTypeTvApp(
                viewModel = viewModel,
                onOpenOidc = { url -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
            )
        }
        intent?.data?.let(viewModel::handleOidcCallback)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let(viewModel::handleOidcCallback)
    }
}
