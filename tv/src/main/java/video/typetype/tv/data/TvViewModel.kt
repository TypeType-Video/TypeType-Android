package video.typetype.tv.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Job
import video.typetype.sdk.core.ServiceId
import video.typetype.sdk.core.TypeTypeClient
import video.typetype.sdk.core.TypeTypeResult
import video.typetype.sdk.core.PlaybackOpenRequest
import video.typetype.sdk.core.LoginCredentials
import video.typetype.sdk.core.RegistrationRequest
import video.typetype.sdk.core.StreamVideo
import video.typetype.sdk.core.SessionSnapshot

public class TvViewModel(
    public val client: TypeTypeClient,
    internal val artifactStore: TvArtifactStore,
    internal val downloadStateStore: TvDownloadStateStore,
    internal val isVideoSupported: (StreamVideo) -> Boolean = { true },
) : ViewModel() {
    internal var pendingOidcAuthorization: TvOidcAuthorization? = null
    internal val mutableState = MutableStateFlow(TvAppState())
    internal var downloadTask: Job? = null
    internal var downloadSession: SessionSnapshot? = null
    public val state: StateFlow<TvAppState> = mutableState.asStateFlow()

    init {
        restoreSession()
    }

    public fun login(identifier: String, password: String) {
        if (identifier.isBlank() || password.isBlank()) {
            mutableState.value = mutableState.value.copy(errorMessage = "Enter your email and password")
            return
        }
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, errorMessage = null)
            when (val result = client.auth.login(LoginCredentials(identifier, null, password))) {
                is TypeTypeResult.Success -> {
                    mutableState.value = mutableState.value.copy(
                        authStatus = TvAuthStatus.AUTHENTICATED,
                        isLoading = false,
                    )
                    viewModelScope.launch { loadAuthenticatedContent() }
                }
                is TypeTypeResult.Failure -> mutableState.value = mutableState.value.copy(
                    isLoading = false,
                    errorMessage = result.error.toUserMessage(),
                )
            }
        }
    }

    public fun register(name: String, email: String, password: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            mutableState.value = mutableState.value.copy(errorMessage = "Enter your name, email and password")
            return
        }
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, errorMessage = null)
            when (val result = client.auth.register(RegistrationRequest(email.trim(), password, name.trim()))) {
                is TypeTypeResult.Success -> {
                    mutableState.value = mutableState.value.copy(
                        authStatus = TvAuthStatus.AUTHENTICATED,
                        isLoading = false,
                    )
                    viewModelScope.launch { loadAuthenticatedContent() }
                }
                is TypeTypeResult.Failure -> mutableState.value = mutableState.value.copy(
                    isLoading = false,
                    errorMessage = result.error.toUserMessage(),
                )
            }
        }
    }

    public fun continueAsGuest() {
        if (mutableState.value.metadata?.guestAllowed != true) return
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, errorMessage = null)
            when (val result = client.auth.guest()) {
                is TypeTypeResult.Success -> {
                    mutableState.value = mutableState.value.copy(
                        authStatus = TvAuthStatus.GUEST,
                        isLoading = false,
                    )
                    viewModelScope.launch { loadHomeContent() }
                }
                is TypeTypeResult.Failure -> mutableState.value = mutableState.value.copy(
                    isLoading = false,
                    errorMessage = result.error.toUserMessage(),
                )
            }
        }
    }

    public fun logout() {
        viewModelScope.launch {
            when (val result = client.auth.logout()) {
                is TypeTypeResult.Success -> mutableState.value = TvAppState(
                    authStatus = TvAuthStatus.SIGNED_OUT,
                    metadata = mutableState.value.metadata,
                    isLoading = false,
                )
                is TypeTypeResult.Failure -> mutableState.value = mutableState.value.copy(
                    errorMessage = result.error.toUserMessage(),
                )
            }
        }
    }

    public fun navigate(destination: TvDestination) {
        mutableState.value = mutableState.value.copy(destination = destination, errorMessage = null)
        if (destination == TvDestination.HOME && mutableState.value.home.isEmpty()) refreshHome()
        if (destination == TvDestination.LIBRARY) loadLibrary()
        if (destination == TvDestination.SEARCH && mutableState.value.searchFilters == null) loadSearchFilters()
    }

    public fun selectService(service: ServiceId) {
        if (service == mutableState.value.selectedService) return
        mutableState.value = mutableState.value.copy(
            selectedService = service,
            home = emptyList(),
            trending = emptyList(),
            shorts = emptyList(),
            searchPage = null,
            searchQuery = "",
            searchSuggestions = emptyList(),
            searchFilters = null,
            selectedSearchContentFilter = null,
            selectedSearchSortFilter = null,
            selectedSearchFilters = emptyMap(),
            isLoading = true,
            errorMessage = null,
        )
        viewModelScope.launch {
            if (mutableState.value.authStatus == TvAuthStatus.AUTHENTICATED) {
                updateUserSettings(mutableState.value.settings.copy(defaultService = service), refreshContent = false)
            }
            loadHomeContent()
        }
    }

    public fun loadLibrary() {
        viewModelScope.launch {
            loadLibraryContent(showLoading = true)
        }
    }

    public fun refreshHome() {
        if (mutableState.value.authStatus == TvAuthStatus.SIGNED_OUT ||
            mutableState.value.authStatus == TvAuthStatus.CHECKING
        ) return
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, errorMessage = null)
            loadHomeContent()
        }
    }

    private fun restoreSession() {
        viewModelScope.launch {
            val metadataResult = withTimeoutOrNull(STARTUP_METADATA_TIMEOUT_MILLISECONDS) {
                client.instance.metadata()
            }
            val metadata = (metadataResult as? TypeTypeResult.Success)?.value
            val metadataError = (metadataResult as? TypeTypeResult.Failure)?.error
            val session = client.sessions.current()
            if (session == null) {
                mutableState.value = mutableState.value.copy(
                    authStatus = TvAuthStatus.SIGNED_OUT,
                    metadata = metadata,
                    isLoading = false,
                    errorMessage = metadataError?.toUserMessage(),
                )
                return@launch
            }
            mutableState.value = mutableState.value.copy(
                authStatus = if (session.isGuest) TvAuthStatus.GUEST else TvAuthStatus.AUTHENTICATED,
                metadata = metadata,
                selectedService = availableTvServices(metadata).firstOrNull {
                    it == mutableState.value.selectedService
                } ?: availableTvServices(metadata).first(),
                isLoading = false,
            )
            resumePendingDownload(session)
            viewModelScope.launch {
                if (session.isGuest) loadHomeContent() else loadAuthenticatedContent()
            }
        }
    }

    public fun startPlayback() {
        val stream = mutableState.value.stream ?: return
        val video = mutableState.value.selectedVideo ?: return
        val standardSession = stream.standardPlaybackSession(mutableState.value.selectedService)
        if (standardSession != null) {
            mutableState.value = mutableState.value.copy(
                playback = standardSession,
                audioOnlyStream = null,
                isLoadingDetails = false,
                errorMessage = null,
            )
            return
        }
        if (mutableState.value.selectedService != ServiceId.YOUTUBE) {
            mutableState.value = mutableState.value.copy(
                errorMessage = "The server did not return a playable HLS or DASH manifest for this service",
            )
            return
        }
        val tracks = stream.selectTvPlaybackTracks(
            isVideoSupported = isVideoSupported,
            preferredVideoItag = mutableState.value.selectedVideoItag,
            preferredAudioItag = mutableState.value.selectedAudioItag,
            preferredAudioTrackId = mutableState.value.selectedAudioTrackId,
            preferredQuality = mutableState.value.settings.defaultQuality,
        )
        if (tracks == null) {
            mutableState.value = mutableState.value.copy(errorMessage = "The TypeType server did not return a playable audio/video pair")
            return
        }
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoadingDetails = true, errorMessage = null)
            when (val result = client.playback.open(
                PlaybackOpenRequest(
                    videoUrl = video.url,
                    videoItag = tracks.video.itag,
                    audioItag = tracks.audio.itag,
                    audioTrackId = tracks.audio.audioTrackId,
                    startTimeMilliseconds = stream.startPositionMilliseconds,
                    isLive = stream.isLive,
                ),
            )) {
                is TypeTypeResult.Success -> mutableState.value = mutableState.value.copy(
                    playback = result.value,
                    audioOnlyStream = null,
                    isLoadingDetails = false,
                )
                is TypeTypeResult.Failure -> mutableState.value = mutableState.value.copy(
                    isLoadingDetails = false,
                    errorMessage = result.error.toUserMessage(),
                )
            }
        }
    }

    public fun closePlayback() {
        mutableState.value = mutableState.value.copy(playback = null, audioOnlyStream = null)
    }

    public fun selectVideoTrack(itag: Int) {
        mutableState.value = mutableState.value.copy(selectedVideoItag = itag)
    }

    public fun selectAudioTrack(itag: Int, trackId: String?) {
        mutableState.value = mutableState.value.copy(
            selectedAudioItag = itag,
            selectedAudioTrackId = trackId,
        )
    }

    public fun selectSubtitle(language: String?, auto: Boolean, name: String? = null) {
        mutableState.value = mutableState.value.copy(
            selectedSubtitleLanguage = language,
            selectedSubtitleAuto = auto,
            selectedSubtitleName = name,
        )
    }

    public fun closeDetails() {
        mutableState.value = mutableState.value.copy(
            stream = null,
            supportedVideoItags = emptySet(),
            selectedVideo = null,
            playback = null,
            audioOnlyStream = null,
            selectedVideoItag = null,
            selectedAudioItag = null,
            selectedAudioTrackId = null,
            selectedSubtitleLanguage = null,
            selectedSubtitleAuto = false,
            selectedSubtitleName = null,
            comments = emptyList(),
            commentsNextPage = null,
            commentsDisabled = false,
            commentReplies = emptyMap(),
        )
    }

}

private const val STARTUP_METADATA_TIMEOUT_MILLISECONDS = 4_000L
