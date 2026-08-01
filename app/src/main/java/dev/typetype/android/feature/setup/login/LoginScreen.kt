package dev.typetype.android.feature.setup.login

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.browser.auth.AuthTabIntent
import androidx.browser.customtabs.CustomTabsClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.net.toUri
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.SectionHeader
import dev.typetype.android.core.ui.components.TypeTypeCard
import dev.typetype.android.core.ui.components.TypeTypePrimaryButton
import dev.typetype.android.core.ui.components.TypeTypeSecondaryButton
import dev.typetype.android.core.ui.components.TypeTypeTextField
import dev.typetype.android.core.ui.components.TypeTypeTextLink
import dev.typetype.android.core.ui.components.RequestIdRow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LoginRoute(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToResetPassword: (String) -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val authLauncher = rememberLauncherForActivityResult(
        contract = AuthTabIntent.AuthenticateUserResultContract(),
    ) { result ->
        if (result.resultCode == AuthTabIntent.RESULT_OK && result.resultUri != null) {
            viewModel.onAction(LoginAction.OnOidcCallback(result.resultUri.toString()))
        } else {
            viewModel.onAction(LoginAction.OnOidcCancelled)
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                LoginEvent.NavigateBack -> onNavigateBack()
                LoginEvent.NavigateToHome -> onNavigateToHome()
                is LoginEvent.NavigateToResetPassword ->
                    onNavigateToResetPassword(event.serverId)
                is LoginEvent.LaunchOidc -> {
                    val provider = CustomTabsClient.getPackageName(context, null)
                    try {
                        AuthTabIntent.Builder()
                            .build()
                            .also { authIntent ->
                                provider?.let(authIntent.intent::setPackage)
                            }
                            .launch(
                                authLauncher,
                                event.authorizationUrl.toUri(),
                                event.redirectScheme,
                            )
                    } catch (_: ActivityNotFoundException) {
                        viewModel.onAction(LoginAction.OnOidcBrowserUnavailable)
                    }
                }
            }
        }
    }
    LoginScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun LoginScreen(
    state: LoginState,
    onAction: (LoginAction) -> Unit,
) {
    val scrollState = rememberScrollState()
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .imePadding(),
        ) {
            IconButton(
                onClick = { onAction(LoginAction.OnBackClick) },
                modifier = Modifier.padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.settings_back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 72.dp, bottom = 24.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.Top,
            ) {
                SectionHeader(text = stringResource(R.string.setup_section))
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.login_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (state.instanceName.isBlank()) {
                        stringResource(R.string.login_subtitle)
                    } else {
                        stringResource(R.string.login_instance_subtitle, state.instanceName)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(28.dp))
                if (state.isLoadingMethods) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                if (state.oidcEnabled) {
                    val provider = state.oidcProviderName?.takeIf(String::isNotBlank)
                    val label = if (provider == null) {
                        stringResource(R.string.login_continue_oidc)
                    } else {
                        stringResource(R.string.login_continue_oidc_provider, provider)
                    }
                    if (state.localLoginEnabled) {
                        TypeTypeSecondaryButton(
                            text = label,
                            onClick = { onAction(LoginAction.OnOidcClick) },
                            enabled = !state.isSubmitting,
                        )
                    } else {
                        TypeTypePrimaryButton(
                            text = label,
                            onClick = { onAction(LoginAction.OnOidcClick) },
                            enabled = !state.isSubmitting,
                            isLoading = state.isSubmitting,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
                if (state.localLoginEnabled) TypeTypeCard {
                    TypeTypeTextField(
                        value = state.identifier,
                        onValueChange = { onAction(LoginAction.OnIdentifierChange(it)) },
                        placeholder = stringResource(R.string.login_identifier_placeholder),
                        enabled = !state.isSubmitting,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    Spacer(Modifier.height(12.dp))
                    TypeTypeTextField(
                        value = state.password,
                        onValueChange = { onAction(LoginAction.OnPasswordChange(it)) },
                        placeholder = stringResource(R.string.login_password_placeholder),
                        enabled = !state.isSubmitting,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { onAction(LoginAction.OnLoginClick) },
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    Spacer(Modifier.height(20.dp))
                    TypeTypePrimaryButton(
                        text = stringResource(R.string.login_title),
                        onClick = { onAction(LoginAction.OnLoginClick) },
                        enabled = !state.isSubmitting,
                        isLoading = state.isSubmitting,
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        TypeTypeTextLink(
                            text = stringResource(R.string.reset_password_title),
                            onClick = { onAction(LoginAction.OnResetPasswordClick) },
                            enabled = !state.isSubmitting,
                        )
                    }
                }
                if (!state.isLoadingMethods &&
                    !state.localLoginEnabled &&
                    !state.oidcEnabled &&
                    !state.guestAllowed
                ) {
                    Text(
                        text = stringResource(R.string.login_no_method),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.errorMessage?.let { error ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    state.errorRequestId?.let { RequestIdRow(requestId = it) }
                }
                if (state.guestAllowed) {
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        TypeTypeTextLink(
                            text = stringResource(R.string.login_continue_guest),
                            onClick = { onAction(LoginAction.OnContinueAsGuestClick) },
                            enabled = !state.isSubmitting,
                        )
                    }
                }
            }
        }
    }
}
