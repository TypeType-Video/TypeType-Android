package dev.typetype.android.services

import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession

@UnstableApi
class PlaybackSessionCallback(
    private val applicationPackageName: String,
    private val applicationUid: Int,
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
            super.onConnect(session, controller)
        } else {
            MediaSession.ConnectionResult.reject()
        }
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
