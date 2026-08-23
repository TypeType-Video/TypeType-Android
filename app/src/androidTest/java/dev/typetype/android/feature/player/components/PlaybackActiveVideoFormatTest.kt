package dev.typetype.android.feature.player.components

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackActiveVideoFormatTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun activeLabelFollowsAdaptiveVideoSizeChanges() {
        val player = AdaptiveFormatTestPlayer(Looper.getMainLooper())
        composeRule.setContent {
            val active = rememberActiveVideoFormat(player)
            Text(listOfNotNull(active?.qualityLabel, active?.codec).joinToString(" · "))
        }

        composeRule.onNodeWithText("720p · 30 fps · H.264").assertIsDisplayed()
        composeRule.runOnIdle { player.selectHeight(1080) }
        composeRule.onNodeWithText("1080p · 60 fps · AV1").assertIsDisplayed()
        composeRule.runOnIdle { player.release() }
    }
}

private class AdaptiveFormatTestPlayer(looper: Looper) : SimpleBasePlayer(looper) {
    private var selectedHeight = 720
    private val tracks = adaptiveTracks()

    override fun getState(): State = State.Builder()
        .setAvailableCommands(Player.Commands.Builder().addAllCommands().build())
        .setPlaybackState(Player.STATE_READY)
        .setVideoSize(VideoSize(if (selectedHeight == 720) 1280 else 1920, selectedHeight))
        .setPlaylist(
            listOf(
                MediaItemData.Builder("adaptive-item")
                    .setMediaItem(MediaItem.Builder().setMediaId("adaptive-item").build())
                    .setTracks(tracks)
                    .build(),
            ),
        )
        .build()

    fun selectHeight(height: Int) {
        selectedHeight = height
        invalidateState()
    }

    override fun handleRelease(): ListenableFuture<*> = Futures.immediateVoidFuture()
}

private fun adaptiveTracks(): Tracks {
    val h264 = Format.Builder()
        .setSampleMimeType(MimeTypes.VIDEO_H264)
        .setCodecs("avc1.64001f")
        .setWidth(1280)
        .setHeight(720)
        .setFrameRate(30f)
        .setAverageBitrate(2_000_000)
        .build()
    val av1 = Format.Builder()
        .setSampleMimeType(MimeTypes.VIDEO_AV1)
        .setCodecs("av01.0.08M.08")
        .setWidth(1920)
        .setHeight(1080)
        .setFrameRate(60f)
        .setAverageBitrate(4_000_000)
        .build()
    return Tracks(
        listOf(
            Tracks.Group(
                TrackGroup(h264, av1),
                true,
                intArrayOf(C.FORMAT_HANDLED, C.FORMAT_HANDLED),
                booleanArrayOf(true, true),
            ),
        ),
    )
}
