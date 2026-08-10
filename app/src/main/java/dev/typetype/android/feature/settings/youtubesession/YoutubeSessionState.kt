package dev.typetype.android.feature.settings.youtubesession

import dev.typetype.android.domain.server.Server
import dev.typetype.android.domain.youtubesession.YoutubeRemoteBrowserPhase
import dev.typetype.android.domain.youtubesession.YoutubeSession
import dev.typetype.android.domain.youtubesession.YoutubeSessionStatus

enum class YoutubeSessionAvailability {
    Checking,
    Available,
    Disabled,
    Unavailable,
}

enum class YoutubeSessionNotice {
    Connected,
    Disconnected,
    SignInCancelled,
}

data class YoutubeSessionState(
    val availability: YoutubeSessionAvailability = YoutubeSessionAvailability.Checking,
    val unavailableReason: String? = null,
    val session: YoutubeSession? = null,
    val isStatusLoading: Boolean = true,
    val isStarting: Boolean = false,
    val isCancelling: Boolean = false,
    val isDisconnecting: Boolean = false,
    val remoteSessionId: String? = null,
    val remoteSessionExpiresAt: Long? = null,
    val remotePhase: YoutubeRemoteBrowserPhase = YoutubeRemoteBrowserPhase.Idle,
    val remoteErrorMessage: String? = null,
    val frameBytes: ByteArray? = null,
    val errorMessage: String? = null,
    val errorRequestId: String? = null,
    val notice: YoutubeSessionNotice? = null,
) {
    val remoteBrowserOpen: Boolean
        get() = remoteSessionId != null

    val canStart: Boolean
        get() = availability == YoutubeSessionAvailability.Available &&
            !isStarting && !remoteBrowserOpen

    val canDisconnect: Boolean
        get() = session?.status?.isStored == true && !isDisconnecting
}

internal fun YoutubeSessionState.clearedForAccountChange() = YoutubeSessionState()

private val YoutubeSessionStatus.isStored: Boolean
    get() = this == YoutubeSessionStatus.Connected || this == YoutubeSessionStatus.NeedsReconnect

internal fun Server?.youtubeSessionAvailability(): YoutubeSessionAvailability = when {
    this == null -> YoutubeSessionAvailability.Checking
    !youtubeRemoteLoginSupported -> YoutubeSessionAvailability.Disabled
    youtubeRemoteLoginReady -> YoutubeSessionAvailability.Available
    youtubeRemoteLoginUnavailableReason == "disabled" -> YoutubeSessionAvailability.Disabled
    else -> YoutubeSessionAvailability.Unavailable
}
