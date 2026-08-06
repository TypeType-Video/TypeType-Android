package dev.typetype.android.services

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.view.KeyEvent
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@UnstableApi
@RunWith(AndroidJUnit4::class)
class PlaybackMediaSessionAndroidTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private lateinit var controller: MediaController
    private lateinit var secondaryController: MediaController
    private lateinit var mediaFile: File

    @Before
    fun connectToPlaybackSession() {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controller = MediaController.Builder(context, token)
            .buildAsync()
            .get(10, TimeUnit.SECONDS)
        secondaryController = MediaController.Builder(context, token)
            .buildAsync()
            .get(10, TimeUnit.SECONDS)
        mediaFile = createSilentWave(context)
    }

    @After
    fun releasePlaybackSession() {
        instrumentation.runOnMainSync {
            controller.stop()
            controller.clearMediaItems()
            secondaryController.release()
            controller.release()
        }
        mediaFile.delete()
    }

    @Test
    fun secondaryControllerPausesTheSameServicePlayer() {
        instrumentation.runOnMainSync {
            controller.setMediaItem(
                MediaItem.Builder()
                    .setUri(Uri.fromFile(mediaFile))
                    .setMimeType(MimeTypes.AUDIO_WAV)
                    .build(),
            )
            controller.prepare()
            controller.play()
        }
        assertTrue(waitForControllerState { it.playWhenReady })

        instrumentation.runOnMainSync { secondaryController.pause() }

        assertTrue(waitForControllerState { !it.playWhenReady })
        assertFalse(readControllerState { it.playWhenReady })
    }

    @Test
    fun hardwareMediaKeysControlTheSharedServicePlayer() {
        instrumentation.runOnMainSync {
            controller.setMediaItem(
                MediaItem.Builder()
                    .setUri(Uri.fromFile(mediaFile))
                    .setMimeType(MimeTypes.AUDIO_WAV)
                    .build(),
            )
            controller.prepare()
            controller.play()
        }
        assertTrue(waitForControllerState { it.playWhenReady })

        val device = UiDevice.getInstance(instrumentation)
        assertTrue(device.pressKeyCode(KeyEvent.KEYCODE_MEDIA_PAUSE))
        assertTrue(waitForControllerState { !it.playWhenReady })

        assertTrue(device.pressKeyCode(KeyEvent.KEYCODE_MEDIA_PLAY))
        assertTrue(waitForControllerState { it.playWhenReady })
    }

    @Test
    fun applicationControllerReceivesTheAudioOnlyCommand() {
        assertTrue(
            readControllerState {
                it.isSessionCommandAvailable(PlaybackAudioOnlyCommand.command)
            },
        )
    }

    @Test
    fun playingMediaPublishesTheSessionNotification() {
        val title = "TypeType notification smoke"
        instrumentation.runOnMainSync {
            controller.setMediaItem(
                MediaItem.Builder()
                    .setUri(Uri.fromFile(mediaFile))
                    .setMimeType(MimeTypes.AUDIO_WAV)
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
                    .build(),
            )
            controller.prepare()
            controller.play()
        }

        assertTrue(
            waitForControllerState {
                it.playWhenReady && it.playbackState == Player.STATE_READY
            },
        )
        val notification = waitForNotification(title)

        assertEquals(Notification.CATEGORY_TRANSPORT, notification?.category)
    }

    @Test
    fun baselineH264VideoAdvancesThroughTheSharedServicePlayer() {
        val video = createSyntheticH264Video(context)
        try {
            instrumentation.runOnMainSync {
                controller.setMediaItem(
                    MediaItem.Builder()
                        .setUri(Uri.fromFile(video))
                        .setMimeType(MimeTypes.VIDEO_MP4)
                        .build(),
                )
                controller.prepare()
                controller.play()
            }

            assertTrue(
                waitForControllerState {
                    it.playbackState == Player.STATE_READY && it.currentPosition >= 250L
                },
            )
        } finally {
            video.delete()
        }
    }

    @Test
    fun playingMediaPromotesTheSharedServiceToForeground() {
        instrumentation.runOnMainSync {
            controller.setMediaItem(
                MediaItem.Builder()
                    .setUri(Uri.fromFile(mediaFile))
                    .setMimeType(MimeTypes.AUDIO_WAV)
                    .build(),
            )
            controller.prepare()
            controller.play()
        }

        assertTrue(
            waitForControllerState {
                it.playWhenReady && it.playbackState == Player.STATE_READY
            },
        )
        assertTrue(waitForForegroundPlaybackService())
    }

    private fun waitForControllerState(condition: (MediaController) -> Boolean): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (System.nanoTime() < deadline) {
            if (readControllerState(condition)) return true
            Thread.sleep(50)
        }
        return readControllerState(condition)
    }

    private fun readControllerState(block: (MediaController) -> Boolean): Boolean {
        val result = AtomicBoolean()
        instrumentation.runOnMainSync { result.set(block(controller)) }
        return result.get()
    }

    private fun waitForNotification(title: String): Notification? {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return null
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (System.nanoTime() < deadline) {
            manager.activeNotifications.firstOrNull { notification ->
                notification.notification.extras
                    .getCharSequence(Notification.EXTRA_TITLE)
                    ?.toString() == title
            }?.let { return it.notification }
            Thread.sleep(50)
        }
        return null
    }

    private fun waitForForegroundPlaybackService(): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (System.nanoTime() < deadline) {
            if (isPlaybackServiceForeground()) return true
            Thread.sleep(50)
        }
        return isPlaybackServiceForeground()
    }

    @Suppress("DEPRECATION")
    private fun isPlaybackServiceForeground(): Boolean {
        val manager = context.getSystemService(ActivityManager::class.java) ?: return false
        val serviceName = ComponentName(context, PlaybackService::class.java)
        return manager.getRunningServices(Int.MAX_VALUE).any {
            it.service == serviceName && it.foreground
        }
    }
}

private fun createSilentWave(context: Context): File {
    val file = File.createTempFile("media-session-", ".wav", context.cacheDir)
    val sampleRate = 8_000
    val sampleCount = sampleRate * 5
    val dataSize = sampleCount * 2
    FileOutputStream(file).use { output ->
        output.write("RIFF".toByteArray())
        output.writeIntLittleEndian(36 + dataSize)
        output.write("WAVEfmt ".toByteArray())
        output.writeIntLittleEndian(16)
        output.writeShortLittleEndian(1)
        output.writeShortLittleEndian(1)
        output.writeIntLittleEndian(sampleRate)
        output.writeIntLittleEndian(sampleRate * 2)
        output.writeShortLittleEndian(2)
        output.writeShortLittleEndian(16)
        output.write("data".toByteArray())
        output.writeIntLittleEndian(dataSize)
        output.write(ByteArray(dataSize))
    }
    return file
}

private fun OutputStream.writeIntLittleEndian(value: Int) {
    repeat(Int.SIZE_BYTES) { shift -> write(value shr (shift * Byte.SIZE_BITS)) }
}

private fun OutputStream.writeShortLittleEndian(value: Int) {
    repeat(Short.SIZE_BYTES) { shift -> write(value shr (shift * Byte.SIZE_BITS)) }
}
