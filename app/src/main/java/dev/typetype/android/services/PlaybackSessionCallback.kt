package dev.typetype.android.services

import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
internal class PlaybackSessionCallback(
    private val applicationPackageName: String,
    private val applicationUid: Int,
    private val playbackBridge: PlaybackAudioOnlyController,
    private val audioOnlyDefaultPolicy: AudioOnlyDefaultPolicy,
) : MediaSession.Callback {
    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        return if (
            acceptsPlaybackController(
                controllerPackageName = controller.packageName,
                controllerUid = controller.uid,
                isTrusted = controller.isTrusted,
                applicationPackageName = applicationPackageName,
                applicationUid = applicationUid,
            )
        ) {
            val accepted = super.onConnect(session, controller)
            if (
                isApplicationPlaybackController(
                    controller.packageName,
                    controller.uid,
                    applicationPackageName,
                    applicationUid,
                )
            ) {
                MediaSession.ConnectionResult.accept(
                    accepted.availableSessionCommands.buildUpon()
                        .add(PlaybackAudioOnlyCommand.command)
                        .build(),
                    accepted.availablePlayerCommands,
                )
            } else {
                accepted
            }
        } else {
            MediaSession.ConnectionResult.reject()
        }
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        if (
            customCommand != PlaybackAudioOnlyCommand.command ||
            !isApplicationPlaybackController(
                controller.packageName,
                controller.uid,
                applicationPackageName,
                applicationUid,
            ) ||
            !args.containsKey(PlaybackAudioOnlyCommand.EXTRA_ENABLED)
        ) {
            return Futures.immediateFuture(
                SessionResult(SessionError.ERROR_NOT_SUPPORTED),
            )
        }
        val defaultRequest = args.getBoolean(PlaybackAudioOnlyCommand.EXTRA_DEFAULT_REQUEST)
        val mediaId = session.player.currentMediaItem?.mediaId
        if (defaultRequest && !audioOnlyDefaultPolicy.shouldApplyDefault(mediaId)) {
            return Futures.immediateFuture(PlaybackAudioOnlyCommand.success())
        }
        val result = PlaybackAudioOnlyCommand.resultFuture()
        playbackBridge.setAudioOnly(
            enabled = args.getBoolean(PlaybackAudioOnlyCommand.EXTRA_ENABLED),
        ) { outcome ->
            result.set(
                outcome.fold(
                    onSuccess = {
                        if (!defaultRequest) {
                            audioOnlyDefaultPolicy.recordManualChoice(mediaId)
                        }
                        PlaybackAudioOnlyCommand.success()
                    },
                    onFailure = PlaybackAudioOnlyCommand::failure,
                ),
            )
        }
        return result
    }
}

internal fun acceptsPlaybackController(
    controllerPackageName: String,
    controllerUid: Int,
    isTrusted: Boolean,
    applicationPackageName: String,
    applicationUid: Int,
): Boolean = isTrusted || (
    controllerPackageName == applicationPackageName && controllerUid == applicationUid
)

internal fun isApplicationPlaybackController(
    controllerPackageName: String,
    controllerUid: Int,
    applicationPackageName: String,
    applicationUid: Int,
): Boolean = controllerPackageName == applicationPackageName && controllerUid == applicationUid
