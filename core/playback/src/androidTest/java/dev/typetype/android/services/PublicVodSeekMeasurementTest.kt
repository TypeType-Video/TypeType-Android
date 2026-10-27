package dev.typetype.android.services

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

@UnstableApi
class PublicVodSeekMeasurementTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun measurePublicVodSeekReadinessWithPinnedTracks() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("publicVodSeekMeasurement") == "true")
        val context = instrumentation.targetContext
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=tXH5EjM_96c"))
                .setPackage(context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        val controller = MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, PlaybackService::class.java)),
        ).buildAsync().get(10, TimeUnit.SECONDS)
        try {
            val deadline = SystemClock.elapsedRealtime() + 40_000L
            while (SystemClock.elapsedRealtime() < deadline && read { controller.currentTracks.groups.isEmpty() }) {
                Thread.sleep(50L)
            }
            val tracks = read {
                controller.currentTracks.groups.flatMap { group ->
                    (0 until group.length).filter(group::isTrackSelected).map { index ->
                        val format = group.getTrackFormat(index)
                        "${format.id}:${format.sampleMimeType}"
                    }
                }
            }
            println("Selected public VOD tracks: $tracks")
            assertTrue("Expected video itag 299, got $tracks", tracks.any { it.startsWith("299:") })
            assertTrue("Expected audio itag 140, got $tracks", tracks.any { it.startsWith("140:") })
            for (target in listOf(999_108L, 225_191L)) {
                val started = SystemClock.elapsedRealtime()
                instrumentation.runOnMainSync {
                    controller.play()
                    controller.seekTo(target)
                }
                var ready = false
                while (SystemClock.elapsedRealtime() - started < 45_000L) {
                    ready = read {
                        controller.playerError == null && controller.isPlaying &&
                            controller.playbackState == Player.STATE_READY &&
                            abs(controller.currentPosition - target) < 4_000L
                    }
                    if (ready) break
                    Thread.sleep(25L)
                }
                println("Android controller ready: targetMs=$target delayMs=${SystemClock.elapsedRealtime() - started}")
                assertTrue("Seek did not recover: $target", ready)
            }
        } finally {
            instrumentation.runOnMainSync { controller.release() }
        }
    }

    private fun <T> read(block: () -> T): T {
        val result = AtomicReference<Result<T>>()
        instrumentation.runOnMainSync { result.set(runCatching(block)) }
        return requireNotNull(result.get()).getOrThrow()
    }
}
