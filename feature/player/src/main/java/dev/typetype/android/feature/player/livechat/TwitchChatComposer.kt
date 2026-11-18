package dev.typetype.android.feature.player.livechat

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.domain.twitch.TwitchAccountRepository
import dev.typetype.android.domain.twitch.TwitchTarget
import dev.typetype.android.domain.twitch.TwitchUrls
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatComposerState(val available: Boolean = false, val sending: Boolean = false, val failed: Boolean = false)

// Sending needs the linked Twitch account; without one the chat stays read-only, as for a guest on the web.
@HiltViewModel
class TwitchChatComposerViewModel @Inject constructor(
    private val accounts: TwitchAccountRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatComposerState())
    val state = _state.asStateFlow()
    private var login: String? = null

    fun start(videoUrl: String) {
        val channel = (TwitchUrls.parse(videoUrl) as? TwitchTarget.Live)?.login ?: return
        if (channel == login) return
        login = channel
        viewModelScope.launch {
            val linked = accounts.status().getOrNull()?.linked == true
            _state.update { it.copy(available = linked) }
        }
    }

    fun send(message: String, onSent: () -> Unit) {
        val channel = login ?: return
        val text = message.trim().takeIf { it.isNotEmpty() } ?: return
        _state.update { it.copy(sending = true, failed = false) }
        viewModelScope.launch {
            accounts.sendChat(channel, text).fold(
                onSuccess = {
                    _state.update { it.copy(sending = false) }
                    onSent()
                },
                onFailure = { _state.update { it.copy(sending = false, failed = true) } },
            )
        }
    }
}

@Composable
internal fun TwitchChatComposer(videoUrl: String, viewModel: TwitchChatComposerViewModel = hiltViewModel(key = "chat-composer")) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel, videoUrl) { viewModel.start(videoUrl) }
    if (!state.available) return
    var text by rememberSaveable(videoUrl) { mutableStateOf("") }
    val send = { viewModel.send(text) { text = "" } }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(MAX_LENGTH) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            enabled = !state.sending,
            isError = state.failed,
            placeholder = { Text(stringResource(R.string.live_chat_send_hint)) },
            supportingText = if (state.failed) ({ Text(stringResource(R.string.live_chat_send_failed)) }) else null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { send() }),
        )
        IconButton(onClick = send, enabled = !state.sending && text.isNotBlank()) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.live_chat_send))
        }
    }
}

// Twitch refuses chat messages longer than 500 characters.
private const val MAX_LENGTH = 500
