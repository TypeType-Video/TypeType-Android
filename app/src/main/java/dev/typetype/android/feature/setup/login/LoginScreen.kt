package dev.typetype.android.feature.setup.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.SectionHeader
import dev.typetype.android.core.ui.components.TypeTypeCard
import dev.typetype.android.core.ui.components.TypeTypePrimaryButton
import dev.typetype.android.core.ui.components.TypeTypeSecondaryButton
import dev.typetype.android.core.ui.components.TypeTypeTextField
import dev.typetype.android.core.ui.components.TypeTypeTextLink
import dev.typetype.android.core.ui.components.RequestIdRow
import dev.typetype.android.feature.setup.auth.rememberOidcAuthLauncher
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LoginRoute(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToRegister: (String) -> Unit,
    onNavigateToResetPassword: (String) -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val launchOidc = rememberOidcAuthLauncher(
        onCallback = { viewModel.onAction(LoginAction.OnOidcCallback(it)) },
        onCancelled = { viewModel.onAction(LoginAction.OnOidcCancelled) },
        onBrowserUnavailable = { viewModel.onAction(LoginAction.OnOidcBrowserUnavailable) },
    )
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                LoginEvent.NavigateBack -> onNavigateBack()
                LoginEvent.NavigateToHome -> onNavigateToHome()
                is LoginEvent.NavigateToRegister -> onNavigateToRegister(event.serverId)
                is LoginEvent.NavigateToResetPassword ->
                    onNavigateToResetPassword(event.serverId)
                is LoginEvent.LaunchOidc ->
                    launchOidc(event.authorizationUrl, event.redirectScheme)
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
        Box(modifier = Modifier.fillMaxSize()) {
            LoginBackdrop()
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
                    .widthIn(max = 480.dp)
                    .align(Alignment.Center)
                    .imePadding()
                    .padding(top = 72.dp, bottom = 24.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.Top,
            ) {
                Surface(
                    modifier = Modifier
                        .size(88.dp)
                        .align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(R.drawable.ic_typetype_brand),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                SectionHeader(text = stringResource(R.string.setup_section))
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        if (state.isReauthentication) R.string.login_again_title
                        else R.string.login_title,
                    ),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (state.isReauthentication) {
                        stringResource(R.string.login_again_subtitle, state.instanceName)
                    } else if (state.instanceName.isBlank()) {
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
                        text = stringResource(
                            if (state.isReauthentication) R.string.login_again_title
                            else R.string.login_title,
                        ),
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
                if (state.registrationAllowed && !state.isReauthentication) {
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        TypeTypeTextLink(
                            text = stringResource(R.string.register_title),
                            onClick = { onAction(LoginAction.OnRegisterClick) },
                            enabled = !state.isSubmitting,
                        )
                    }
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

@Composable
private fun LoginBackdrop() {
    val colors = MaterialTheme.colorScheme
    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = maxOf(size.width, size.height) * 0.7f
        drawCircle(
            color = colors.primary.copy(alpha = 0.12f),
            radius = radius,
            center = Offset(size.width * 1.02f, -size.height * 0.08f),
        )
        drawCircle(
            color = colors.tertiary.copy(alpha = 0.09f),
            radius = radius * 0.72f,
            center = Offset(-size.width * 0.08f, size.height * 0.98f),
        )
        drawCircle(
            color = Color.White.copy(
                alpha = if (colors.background.luminance() > 0.5f) {
                    0.08f
                } else {
                    0.025f
                },
            ),
            radius = radius * 0.42f,
            center = Offset(size.width * 0.78f, size.height * 0.72f),
        )
    }
}
