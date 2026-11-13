package dev.typetype.android.feature.settings.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R

@Composable
internal fun DeviceLinkSection(viewModel: DeviceLinkViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var code by rememberSaveable { mutableStateOf("") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.device_link_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            val request = state.request
            when {
                request != null -> {
                    Text(
                        text = stringResource(R.string.device_link_question, request.deviceName.ifBlank { request.userCode }),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::approve, enabled = !state.busy) {
                            Text(stringResource(R.string.device_link_approve))
                        }
                        OutlinedButton(onClick = viewModel::deny, enabled = !state.busy) {
                            Text(stringResource(R.string.device_link_deny))
                        }
                    }
                }
                state.outcome != null -> {
                    Text(
                        text = stringResource(
                            if (state.outcome == DeviceLinkOutcome.Approved) R.string.device_link_approved
                            else R.string.device_link_denied,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = {
                        code = ""
                        viewModel.reset()
                    }) { Text(stringResource(R.string.device_link_another)) }
                }
                else -> {
                    Text(stringResource(R.string.device_link_hint), style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.take(MAX_CODE_LENGTH) },
                        singleLine = true,
                        label = { Text(stringResource(R.string.device_link_code)) },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Go,
                        ),
                        keyboardActions = KeyboardActions(onGo = { if (code.isNotBlank()) viewModel.lookup(code) }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(onClick = { viewModel.lookup(code) }, enabled = code.isNotBlank() && !state.busy) {
                        Text(stringResource(R.string.device_link_continue))
                    }
                }
            }
            state.errorMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private const val MAX_CODE_LENGTH = 16
