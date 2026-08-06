package dev.typetype.android.feature.setup.auth

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.auth.AuthTabIntent
import androidx.browser.customtabs.CustomTabsClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import kotlinx.coroutines.delay

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
    val browserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        currentCancelled()
    }
    val authLauncher = rememberLauncherForActivityResult(
        contract = AuthTabIntent.AuthenticateUserResultContract(),
    ) { result ->
        if (result.resultCode == AuthTabIntent.RESULT_OK && result.resultUri != null) {
            currentCallback(result.resultUri.toString())
        } else {
            currentCancelled()
        }
    }
    return remember(context, authLauncher, browserLauncher) {
        { authorizationUrl, redirectScheme ->
            val provider = CustomTabsClient.getPackageName(context, null)
            val launched = launchOidcWithBrowserFallback(
                launchAuthTab = {
                    try {
                        AuthTabIntent.Builder()
                            .build()
                            .also { authIntent -> provider?.let(authIntent.intent::setPackage) }
                            .launch(authLauncher, authorizationUrl.toUri(), redirectScheme)
                        true
                    } catch (_: ActivityNotFoundException) {
                        false
                    }
                },
                launchBrowser = {
                    val browserIntent = Intent(Intent.ACTION_VIEW, authorizationUrl.toUri()).apply {
                        addCategory(Intent.CATEGORY_BROWSABLE)
                        provider?.let(::setPackage)
                    }
                    try {
                        browserLauncher.launch(browserIntent)
                        true
                    } catch (_: ActivityNotFoundException) {
                        false
                    }
                },
            )
            if (!launched) {
                currentUnavailable()
            }
        }
    }
}

internal fun launchOidcWithBrowserFallback(
    launchAuthTab: () -> Boolean,
    launchBrowser: () -> Boolean,
): Boolean {
    if (launchAuthTab()) {
        return true
    }
    return launchBrowser()
}

internal suspend fun cancelOidcAfterBrowserReturn(
    hasCallback: () -> Boolean,
    cancel: suspend () -> Unit,
    delayMillis: Long = OIDC_CALLBACK_GRACE_MILLIS,
) {
    delay(delayMillis)
    if (!hasCallback()) {
        cancel()
    }
}

private const val OIDC_CALLBACK_GRACE_MILLIS = 3_000L
