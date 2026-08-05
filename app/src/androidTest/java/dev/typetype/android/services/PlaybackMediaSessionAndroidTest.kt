package dev.typetype.android.services

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.After
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
