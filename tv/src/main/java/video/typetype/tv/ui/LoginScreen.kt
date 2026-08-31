package video.typetype.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import video.typetype.tv.data.TvAppState
import video.typetype.tv.data.TvAuthStatus
import video.typetype.tv.R

@Composable
public fun LoginScreen(
    state: TvAppState,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    onOidc: () -> Unit,
    onContinueAsGuest: () -> Unit,
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var registering by remember { mutableStateOf(false) }
    val nameFocus = remember { FocusRequester() }
    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val loginFocus = remember { FocusRequester() }
    val oidcFocus = remember { FocusRequester() }
    val guestFocus = remember { FocusRequester() }
    val registrationFocus = remember { FocusRequester() }
    val createAccountFocus = remember { FocusRequester() }
    var focusedMode by remember { mutableStateOf<Boolean?>(null) }
    var editingInput by remember { mutableStateOf<LoginInput?>(null) }
    LaunchedEffect(state.authStatus, state.isLoading, state.metadata, registering) {
        if (state.authStatus == TvAuthStatus.SIGNED_OUT && !state.isLoading && focusedMode != registering) {
            withFrameNanos { }
            val requested = when {
                registering -> registrationFocus.requestFocus()
                state.metadata?.localLoginEnabled != false -> loginFocus.requestFocus()
                state.metadata.oidcEnabled -> oidcFocus.requestFocus()
                state.metadata.guestAllowed -> guestFocus.requestFocus()
                else -> false
            }
            if (requested) focusedMode = registering
        }
    }
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = .32f), Color(0xFF101218), Color(0xFF08090C)),
                center = androidx.compose.ui.geometry.Offset(250f, 380f),
                radius = 1_150f,
            ),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 72.dp, vertical = 52.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LoginIntroduction(state)
            Surface(
                modifier = Modifier.width(410.dp),
                shape = RoundedCornerShape(18.dp),
                colors = androidx.tv.material3.SurfaceDefaults.colors(
                    containerColor = Color(0xFF1A1D24).copy(alpha = .96f),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 30.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    Text(
                        if (registering) "Create account" else "Sign in",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        if (registering) "Create an account on this TypeType instance."
                        else "Use your TypeType account on this TV.",
                        color = Color.White.copy(alpha = .72f),
                    )
                    Spacer(Modifier.height(3.dp))
                    if (registering) {
                        TvLoginField(
                            displayName, "Display name", nameFocus,
                            onClick = { editingInput = LoginInput.NAME },
                        )
                        TvLoginField(identifier, "Email", emailFocus) {
                            editingInput = LoginInput.IDENTIFIER
                        }
                        TvLoginField(
                            password, "Password", passwordFocus, password = true,
                            onClick = { editingInput = LoginInput.PASSWORD },
                        )
                        Button(
                            modifier = Modifier.fillMaxWidth().focusRequester(registrationFocus),
                            enabled = !state.isLoading,
                            onClick = {
                                when {
                                    displayName.isBlank() -> editingInput = LoginInput.NAME
                                    identifier.isBlank() -> editingInput = LoginInput.IDENTIFIER
                                    password.isBlank() -> editingInput = LoginInput.PASSWORD
                                    else -> onRegister(displayName, identifier, password)
                                }
                            },
                            colors = ButtonDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            CenteredLoginButtonText(if (state.isLoading) "Creating account" else "Create account")
                        }
                        Button(modifier = Modifier.fillMaxWidth(), onClick = { registering = false }) {
                            CenteredLoginButtonText("Back to sign in")
                        }
                    } else if (state.metadata?.localLoginEnabled != false) {
                        TvLoginField(
                            identifier, "Email or username", emailFocus,
                            onClick = { editingInput = LoginInput.IDENTIFIER },
                        )
                        TvLoginField(
                            password, "Password", passwordFocus, password = true,
                            onClick = { editingInput = LoginInput.PASSWORD },
                        )
                        Button(
                            modifier = Modifier.fillMaxWidth().focusRequester(loginFocus).focusProperties {
                                up = passwordFocus
                                if (state.metadata?.oidcEnabled == true) down = oidcFocus
                                else if (state.metadata?.guestAllowed == true) down = guestFocus
                            },
                            enabled = !state.isLoading,
                            onClick = {
                                when {
                                    identifier.isBlank() -> editingInput = LoginInput.IDENTIFIER
                                    password.isBlank() -> editingInput = LoginInput.PASSWORD
                                    else -> onLogin(identifier.trim(), password)
                                }
                            },
                            colors = ButtonDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            CenteredLoginButtonText(
                                if (state.isLoading) "Connecting" else "Continue",
                                FontWeight.SemiBold,
                            )
                        }
                    }
                    if (state.metadata?.oidcEnabled == true) {
                        Button(
                            modifier = Modifier.fillMaxWidth().focusRequester(oidcFocus),
                            enabled = !state.isLoading,
                            onClick = onOidc,
                        ) {
                            CenteredLoginButtonText("Continue with ${state.metadata.oidcProviderName ?: "single sign-on"}")
                        }
                    }
                    if (state.metadata?.guestAllowed == true) {
                        Button(
                            modifier = Modifier.fillMaxWidth().focusRequester(guestFocus),
                            enabled = !state.isLoading,
                            onClick = onContinueAsGuest,
                        ) { CenteredLoginButtonText("Watch as guest") }
                    }
                    if (!registering && state.metadata?.registrationAllowed == true &&
                        state.metadata.localLoginEnabled
                    ) {
                        Button(
                            modifier = Modifier.fillMaxWidth().focusRequester(createAccountFocus),
                            onClick = { registering = true },
                        ) {
                            CenteredLoginButtonText("Create an account")
                        }
                    }
                    state.errorMessage?.let { Text(it, color = Color(0xFFFFB4AB), maxLines = 3) }
                }
            }
        }
    }
    editingInput?.let { input ->
        TvTextPrompt(
            title = when (input) {
                LoginInput.NAME -> "Display name"
                LoginInput.IDENTIFIER -> if (registering) "Email" else "Email or username"
                LoginInput.PASSWORD -> "Password"
            },
            initialValue = when (input) {
                LoginInput.NAME -> displayName
                LoginInput.IDENTIFIER -> identifier
                LoginInput.PASSWORD -> password
            },
            password = input == LoginInput.PASSWORD,
            trimValue = input != LoginInput.PASSWORD,
            actionLabel = if (input == LoginInput.PASSWORD) "Done" else "Next",
            onDismiss = { editingInput = null },
            onSubmit = { value ->
                when (input) {
                    LoginInput.NAME -> displayName = value
                    LoginInput.IDENTIFIER -> identifier = value
                    LoginInput.PASSWORD -> password = value
                }
                editingInput = when {
                    registering && input == LoginInput.NAME -> LoginInput.IDENTIFIER
                    input == LoginInput.IDENTIFIER -> LoginInput.PASSWORD
                    else -> null
                }
                if (editingInput == null) {
                    if (registering) registrationFocus.requestFocus() else loginFocus.requestFocus()
                }
            },
        )
    }
}

@Composable
private fun CenteredLoginButtonText(
    label: String,
    weight: FontWeight = FontWeight.Medium,
) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(label, fontWeight = weight, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LoginIntroduction(state: TvAppState) {
    Column(modifier = Modifier.width(430.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Image(
                painter = painterResource(R.drawable.ic_typetype),
                contentDescription = "TypeType",
                modifier = Modifier.width(58.dp).height(58.dp),
            )
            Text(
                "TYPETYPE",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
            )
        }
        Text(
            state.metadata?.tagline ?: "Your videos. Your instance. Your screen.",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        state.metadata?.name?.let {
            Text("Connected to $it", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = .7f))
        }
    }
}
