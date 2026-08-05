package dev.typetype.android.services

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.SettableFuture
import dev.typetype.android.core.error.CodedFailure
import java.io.IOException

@OptIn(markerClass = [UnstableApi::class])
internal object PlaybackAudioOnlyCommand {
    const val ACTION = "dev.typetype.android.SET_AUDIO_ONLY"
    const val EXTRA_ENABLED = "enabled"
    const val EXTRA_FAILURE_CODE = "failure_code"
    const val EXTRA_REQUEST_ID = "request_id"

    val command = SessionCommand(ACTION, Bundle.EMPTY)

    fun arguments(enabled: Boolean) = Bundle().apply {
        putBoolean(EXTRA_ENABLED, enabled)
    }

    fun resultFuture(): SettableFuture<SessionResult> = SettableFuture.create()

    fun success(): SessionResult = SessionResult(SessionResult.RESULT_SUCCESS)

    fun failure(error: Throwable): SessionResult {
        val coded = error as? CodedFailure
        val extras = Bundle().apply {
            coded?.failureCode?.takeIf { it.isNotBlank() }?.let {
                putString(EXTRA_FAILURE_CODE, it)
            }
            coded?.requestId?.takeIf { it.isNotBlank() }?.let {
                putString(EXTRA_REQUEST_ID, it)
            }
        }
        val resultCode = when {
            error is AudioOnlyUnavailableFailure ->
                SessionError.ERROR_NOT_SUPPORTED
            coded?.statusCode in setOf(400, 404, 422) ->
                SessionError.ERROR_NOT_SUPPORTED
            error is IOException || coded?.statusCode == 408 || coded?.statusCode == 429 ||
                coded?.statusCode?.let { it >= 500 } == true ->
                SessionError.ERROR_IO
            else -> SessionError.ERROR_INVALID_STATE
        }
        return SessionResult(resultCode, extras)
    }
}

internal class AudioOnlyUnavailableFailure :
    IllegalStateException("Audio-only playback is unavailable")

internal class AudioOnlyInactivePlaybackFailure :
    IllegalStateException("No active audio-only playback session")
