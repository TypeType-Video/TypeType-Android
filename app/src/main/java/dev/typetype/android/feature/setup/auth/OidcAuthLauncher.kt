package dev.typetype.android.feature.setup.auth

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.browser.auth.AuthTabIntent
import androidx.browser.customtabs.CustomTabsClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

@Composable
fun rememberOidcAuthLauncher(
    onCallback: (String) -> Unit,
    onCancelled: () -> Unit,
    onBrowserUnavailable: () -> Unit,
): (String, String) -> Unit {
    val context = LocalContext.current
    val currentCallback by rememberUpdatedState(onCallback)
    val currentCancelled by rememberUpdatedState(onCancelled)
    val currentUnavailable by rememberUpdatedState(onBrowserUnavailable)
    val launcher = rememberLauncherForActivityResult(
        contract = AuthTabIntent.AuthenticateUserResultContract(),
    ) { result ->
        if (result.resultCode == AuthTabIntent.RESULT_OK && result.resultUri != null) {
            currentCallback(result.resultUri.toString())
        } else {
            currentCancelled()
        }
    }
    return remember(context, launcher) {
        { authorizationUrl, redirectScheme ->
            val provider = CustomTabsClient.getPackageName(context, null)
            try {
                AuthTabIntent.Builder()
                    .build()
                    .also { authIntent -> provider?.let(authIntent.intent::setPackage) }
                    .launch(launcher, authorizationUrl.toUri(), redirectScheme)
            } catch (_: ActivityNotFoundException) {
                currentUnavailable()
            }
        }
    }
}
