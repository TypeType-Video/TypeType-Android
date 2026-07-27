package dev.typetype.android.feature.setup.addserver

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.SectionHeader
import dev.typetype.android.core.ui.components.TypeTypePrimaryButton
import dev.typetype.android.core.ui.components.TypeTypeTextField
import dev.typetype.android.core.ui.components.RequestIdRow
import dev.typetype.android.domain.setup.ServerAddress
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AddServerRoute(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: (serverId: String) -> Unit,
    viewModel: AddServerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onAction(
            if (granted) AddServerAction.OnConnectClick
            else AddServerAction.OnLocalNetworkPermissionDenied,
        )
    }
    val onConnect = {
        val needsPermission = Build.VERSION.SDK_INT >= 37 &&
            ServerAddress.requiresLocalNetworkAccess(state.url) &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_LOCAL_NETWORK,
            ) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        } else {
            viewModel.onAction(AddServerAction.OnConnectClick)
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                AddServerEvent.NavigateBack -> onNavigateBack()
                is AddServerEvent.NavigateToLogin -> onNavigateToLogin(event.serverId)
            }
        }
    }
    AddServerScreen(
        state = state,
        onAction = viewModel::onAction,
        onConnect = onConnect,
    )
}

@Composable
fun AddServerScreen(
    state: AddServerState,
    onAction: (AddServerAction) -> Unit,
    onConnect: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

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
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 480.dp)
                    .align(Alignment.Center)
                    .verticalScroll(scrollState),
            ) {
                Surface(
                    modifier = Modifier
                        .size(96.dp)
                        .padding(4.dp)
                        .align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(R.drawable.ic_typetype_brand),
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
                SectionHeader(text = stringResource(R.string.setup_section))
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.setup_add_server_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(20.dp))
                TypeTypeTextField(
                    value = state.url,
                    onValueChange = { onAction(AddServerAction.OnUrlChange(it)) },
                    placeholder = stringResource(R.string.setup_server_placeholder),
                    enabled = !state.isConnecting,
                    isError = state.errorMessage != null || state.localNetworkPermissionDenied,
                    supportingText = if (state.localNetworkPermissionDenied) {
                        stringResource(R.string.setup_local_network_permission_denied)
                    } else {
                        state.errorMessage
                    },
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(onGo = { onConnect() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
                state.errorRequestId?.let { RequestIdRow(requestId = it) }
                if (ServerAddress.usesCleartextHttp(state.url)) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.setup_cleartext_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (state.resolvedName != null) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = state.resolvedName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!state.resolvedTagline.isNullOrBlank()) {
                        Text(
                            text = state.resolvedTagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    state.resolvedVersion?.let {
                        Text(
                            text = stringResource(R.string.setup_server_version, it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                TypeTypePrimaryButton(
                    text = stringResource(R.string.setup_connect),
                    onClick = onConnect,
                    enabled = state.url.isNotBlank(),
                    isLoading = state.isConnecting,
                )
            }
            IconButton(
                onClick = { onAction(AddServerAction.OnBackClick) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.settings_back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}
