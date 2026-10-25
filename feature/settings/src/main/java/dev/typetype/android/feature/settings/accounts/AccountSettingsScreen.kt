package dev.typetype.android.feature.settings.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.feature.settings.SettingsDetailTopBar
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AccountSettingsRoute(
    onNavigateBack: () -> Unit,
    onAccountActivated: () -> Unit,
    onSignIn: (String, String?) -> Unit,
    onAddInstance: () -> Unit,
    viewModel: AccountSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                AccountSettingsEvent.AccountActivated -> onAccountActivated()
                is AccountSettingsEvent.SignIn -> onSignIn(event.serverId, event.accountId)
                AccountSettingsEvent.AddInstance -> onAddInstance()
            }
        }
    }
    AccountSettingsScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onSelect = viewModel::select,
        onForget = viewModel::forget,
        onSignIn = viewModel::signIn,
        onAddInstance = viewModel::addInstance,
    )
}

@Composable
private fun AccountSettingsScreen(
    state: AccountSettingsState,
    onNavigateBack: () -> Unit,
    onSelect: (String, String) -> Unit,
    onForget: (String, String) -> Unit,
    onSignIn: (String, String?) -> Unit,
    onAddInstance: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsDetailTopBar(
                title = stringResource(R.string.accounts_title),
                onNavigateBack = onNavigateBack,
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f),
            ) {
                state.errorMessage?.let { message ->
                    item { AccountSettingsError(message, state.errorRequestId) }
                }
                state.servers.forEach { server ->
                    val accounts = state.accounts.filter { it.serverId == server.id }
                    item(key = server.id) {
                        InstanceAccountsCard(
                            server = server,
                            accounts = accounts,
                            activeScope = state.activeScope,
                            busyAccountId = state.busyAccountId,
                            onSelect = onSelect,
                            onForget = onForget,
                            onSignIn = { accountId -> onSignIn(server.id, accountId) },
                        )
                    }
                }
                item {
                    Button(
                        onClick = onAddInstance,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.accounts_add_instance))
                    }
                }
            }
        }
    }
}
