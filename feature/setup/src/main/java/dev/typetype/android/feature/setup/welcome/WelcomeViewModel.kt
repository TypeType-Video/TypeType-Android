package dev.typetype.android.feature.setup.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class WelcomeViewModel @Inject constructor() : ViewModel() {

    private val eventsChannel = Channel<WelcomeEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    fun onAction(action: WelcomeAction) {
        when (action) {
            WelcomeAction.OnGetStartedClick -> emit(WelcomeEvent.NavigateToAddServer)
        }
    }

    private fun emit(event: WelcomeEvent) {
        viewModelScope.launch { eventsChannel.send(event) }
    }
}
