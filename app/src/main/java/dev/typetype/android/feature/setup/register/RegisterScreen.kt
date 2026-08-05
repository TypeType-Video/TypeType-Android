package dev.typetype.android.feature.setup.register

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.RequestIdRow
import dev.typetype.android.core.ui.components.SectionHeader
import dev.typetype.android.core.ui.components.TypeTypeCard
import dev.typetype.android.core.ui.components.TypeTypePrimaryButton
import dev.typetype.android.core.ui.components.TypeTypeSecondaryButton
import dev.typetype.android.core.ui.components.TypeTypeTextField
import dev.typetype.android.core.ui.components.TypeTypeTextLink
import dev.typetype.android.feature.setup.auth.rememberOidcAuthLauncher
import kotlinx.coroutines.flow.collectLatest

@Composable
fun RegisterRoute(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val launchOidc = rememberOidcAuthLauncher(
        onCallback = { viewModel.onAction(RegisterAction.OnOidcCallback(it)) },
        onCancelled = { viewModel.onAction(RegisterAction.OnOidcCancelled) },
        onBrowserUnavailable = {
            viewModel.onAction(RegisterAction.OnOidcBrowserUnavailable)
        },
    )
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                RegisterEvent.NavigateBack -> onNavigateBack()
                RegisterEvent.NavigateToHome -> onNavigateToHome()
                is RegisterEvent.LaunchOidc ->
                    launchOidc(event.authorizationUrl, event.redirectScheme)
            }
        }
    }
    RegisterScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun RegisterScreen(
    state: RegisterState,
    onAction: (RegisterAction) -> Unit,
) {
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
                onClick = { onAction(RegisterAction.OnBackClick) },
                modifier = Modifier.padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.settings_back),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 72.dp, bottom = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top,
            ) {
                SectionHeader(text = stringResource(R.string.setup_section))
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        if (state.bootstrapAvailable) R.string.register_admin_title
                        else R.string.register_title,
                    ),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp,
                    ),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = registerSubtitle(state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(28.dp))
                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    RegisterOptions(state = state, onAction = onAction)
                }
                state.errorMessage?.let { error ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    state.errorRequestId?.let { RequestIdRow(requestId = it) }
                    if (!state.isRegistrationOpen && !state.isSubmitting) {
                        Spacer(Modifier.height(12.dp))
                        TypeTypeSecondaryButton(
                            text = stringResource(R.string.action_retry),
                            onClick = { onAction(RegisterAction.OnRetryClick) },
                        )
                    }
                }
                if (!state.bootstrapAvailable) {
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        TypeTypeTextLink(
                            text = stringResource(R.string.register_sign_in),
                            onClick = { onAction(RegisterAction.OnBackClick) },
                            enabled = !state.isSubmitting,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RegisterOptions(
    state: RegisterState,
    onAction: (RegisterAction) -> Unit,
) {
    if (state.oidcEnabled) {
        val provider = state.oidcProviderName?.takeIf(String::isNotBlank)
        val label = if (provider == null) {
            stringResource(R.string.login_continue_oidc)
        } else {
            stringResource(R.string.login_continue_oidc_provider, provider)
        }
        if (state.isRegistrationOpen) {
            TypeTypeSecondaryButton(
                text = label,
                onClick = { onAction(RegisterAction.OnOidcClick) },
                enabled = !state.isSubmitting,
            )
        } else {
            TypeTypePrimaryButton(
                text = label,
                onClick = { onAction(RegisterAction.OnOidcClick) },
                enabled = !state.isSubmitting,
                isLoading = state.isSubmitting,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
    if (state.isRegistrationOpen) {
        RegisterForm(state = state, onAction = onAction)
    } else if (!state.oidcEnabled) {
        Text(
            text = stringResource(
                if (state.localLoginEnabled) R.string.register_closed
                else R.string.register_local_disabled,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RegisterForm(
    state: RegisterState,
    onAction: (RegisterAction) -> Unit,
) {
    TypeTypeCard {
        TypeTypeTextField(
            value = state.name,
            onValueChange = { onAction(RegisterAction.OnNameChange(it)) },
            placeholder = stringResource(
                if (state.bootstrapAvailable) R.string.register_admin_name
                else R.string.register_name,
            ),
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(12.dp))
        TypeTypeTextField(
            value = state.email,
            onValueChange = { onAction(RegisterAction.OnEmailChange(it)) },
            placeholder = stringResource(R.string.register_email),
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
        )
        Spacer(Modifier.height(12.dp))
        TypeTypeTextField(
            value = state.password,
            onValueChange = { onAction(RegisterAction.OnPasswordChange(it)) },
            placeholder = stringResource(R.string.login_password_placeholder),
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { onAction(RegisterAction.OnRegisterClick) },
            ),
            visualTransformation = PasswordVisualTransformation(),
        )
        Spacer(Modifier.height(20.dp))
        TypeTypePrimaryButton(
            text = stringResource(
                if (state.bootstrapAvailable) R.string.register_admin_action
                else R.string.register_action,
            ),
            onClick = { onAction(RegisterAction.OnRegisterClick) },
            enabled = !state.isSubmitting,
            isLoading = state.isSubmitting,
        )
    }
}

@Composable
private fun registerSubtitle(state: RegisterState): String = when {
    state.bootstrapAvailable -> stringResource(
        R.string.register_admin_subtitle,
        state.instanceName,
    )
    state.isClosed -> stringResource(R.string.register_closed)
    state.instanceName.isBlank() -> stringResource(R.string.register_subtitle)
    else -> stringResource(R.string.register_instance_subtitle, state.instanceName)
}
