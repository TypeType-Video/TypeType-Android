package dev.typetype.android.feature.settings.linkedaccounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.domain.linkedsessions.BiliBiliQrLogin
import dev.typetype.android.domain.linkedsessions.BiliBiliQrPoll
import dev.typetype.android.domain.linkedsessions.BiliBiliSessionRepository
import dev.typetype.android.domain.linkedsessions.BiliBiliSessionState
import dev.typetype.android.domain.linkedsessions.YoutubeDeviceLogin
import dev.typetype.android.domain.linkedsessions.YoutubeDeviceLoginRepository
import dev.typetype.android.domain.linkedsessions.YoutubeDevicePoll
import dev.typetype.android.domain.youtubesession.YoutubeSessionRepository
import dev.typetype.android.domain.youtubesession.YoutubeSessionStatus
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LinkedAccountsState(
    val youtubeStatus: YoutubeSessionStatus? = null,
    val youtubeLogin: YoutubeDeviceLogin? = null,
    val youtubeError: String? = null,
    val biliBiliState: BiliBiliSessionState? = null,
    val biliBiliQr: BiliBiliQrLogin? = null,
    val biliBiliScanned: Boolean = false,
    val biliBiliError: String? = null,
    val busy: Boolean = false,
)

@HiltViewModel
class LinkedAccountsViewModel @Inject constructor(
    private val youtubeSessions: YoutubeSessionRepository,
    private val youtubeDevice: YoutubeDeviceLoginRepository,
    private val biliBili: BiliBiliSessionRepository,
    private val errorMapper: UserErrorMapper,
) : ViewModel() {
    private val _state = MutableStateFlow(LinkedAccountsState())
    val state: StateFlow<LinkedAccountsState> = _state.asStateFlow()
    private var youtubeJob: Job? = null
    private var biliBiliJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            youtubeSessions.getStatus().onSuccess { session -> _state.update { it.copy(youtubeStatus = session.status) } }
        }
        viewModelScope.launch {
            biliBili.state().onSuccess { value -> _state.update { it.copy(biliBiliState = value) } }
        }
    }

    fun startYoutube() {
        youtubeJob?.cancel()
        _state.update { it.copy(youtubeError = null, youtubeLogin = null) }
        youtubeJob = viewModelScope.launch {
            val login = youtubeDevice.start().getOrElse { return@launch youtubeFailure(it) }
            _state.update { it.copy(youtubeLogin = login) }
            var interval = login.intervalSeconds * 1_000L
            while (true) {
                delay(interval)
                when (youtubeDevice.poll().getOrElse { return@launch youtubeFailure(it) }) {
                    YoutubeDevicePoll.Pending -> Unit
                    YoutubeDevicePoll.SlowDown -> interval += SLOW_DOWN_STEP_MS
                    YoutubeDevicePoll.Connected -> {
                        _state.update { it.copy(youtubeLogin = null, youtubeStatus = YoutubeSessionStatus.Connected) }
                        return@launch
                    }
                }
            }
        }
    }

    fun cancelYoutube() {
        if (youtubeJob?.isActive != true) return
        youtubeJob?.cancel()
        _state.update { it.copy(youtubeLogin = null) }
        viewModelScope.launch { youtubeDevice.cancel() }
    }

    fun disconnectYoutube() = write(::youtubeFailure) {
        youtubeSessions.disconnect().getOrThrow()
        _state.update { it.copy(youtubeStatus = YoutubeSessionStatus.Disconnected) }
    }

    fun startBiliBili() {
        biliBiliJob?.cancel()
        _state.update { it.copy(biliBiliError = null, biliBiliQr = null, biliBiliScanned = false) }
        biliBiliJob = viewModelScope.launch {
            val qr = biliBili.startQrLogin().getOrElse { return@launch biliBiliFailure(it) }
            _state.update { it.copy(biliBiliQr = qr) }
            while (true) {
                delay(BILIBILI_POLL_MS)
                when (biliBili.pollQrLogin(qr.qrcodeKey).getOrElse { return@launch biliBiliFailure(it) }) {
                    BiliBiliQrPoll.Waiting -> Unit
                    BiliBiliQrPoll.Scanned -> _state.update { it.copy(biliBiliScanned = true) }
                    BiliBiliQrPoll.Confirmed -> {
                        _state.update { it.copy(biliBiliQr = null, biliBiliState = BiliBiliSessionState.Connected) }
                        return@launch
                    }
                    BiliBiliQrPoll.Expired -> return@launch biliBiliMessage(R.string.linked_accounts_qr_expired)
                    BiliBiliQrPoll.Failed -> return@launch biliBiliMessage(R.string.linked_accounts_failed)
                }
            }
        }
    }

    fun cancelBiliBili() {
        biliBiliJob?.cancel()
        _state.update { it.copy(biliBiliQr = null, biliBiliScanned = false) }
    }

    fun disconnectBiliBili() = write(::biliBiliFailure) {
        biliBili.disconnect().getOrThrow()
        _state.update { it.copy(biliBiliState = BiliBiliSessionState.Disconnected) }
    }

    private fun write(onFailure: (Throwable) -> Unit, block: suspend () -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                onFailure(failure)
            }
            _state.update { it.copy(busy = false) }
        }
    }

    private fun youtubeFailure(failure: Throwable) = _state.update {
        it.copy(youtubeLogin = null, youtubeError = errorMapper.details(failure, R.string.linked_accounts_failed).message)
    }

    private fun biliBiliFailure(failure: Throwable) = _state.update {
        it.copy(biliBiliQr = null, biliBiliError = errorMapper.details(failure, R.string.linked_accounts_failed).message)
    }

    private fun biliBiliMessage(resource: Int) = _state.update {
        it.copy(biliBiliQr = null, biliBiliError = errorMapper.message(failureCode = null, statusCode = null, fallbackRes = resource))
    }

    private companion object {
        const val SLOW_DOWN_STEP_MS = 5_000L
        const val BILIBILI_POLL_MS = 2_000L
    }
}
