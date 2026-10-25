package dev.typetype.android.services

import android.net.Uri
import android.os.Bundle
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.typetype.android.domain.stream.AudioOnlyStream
import dev.typetype.android.domain.stream.AudioOnlyStreamKind
import dev.typetype.android.domain.stream.AudioOnlyStreamRepository
import dev.typetype.android.domain.stream.StreamRequestScope
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProviderAudioOnlyServiceBridgeTest {
    @Test
    fun switchingToAudioOnlyPreservesPlaybackAndRestoresTheOriginalItem() {
        val repository = RecordingAudioOnlyRepository(audioStream("first"))
        val original = providerMediaItem()
        lateinit var player: AudioOnlyTestPlayer
        lateinit var bridge: ProviderAudioOnlyServiceBridge
        onMain {
            player = AudioOnlyTestPlayer(original, positionMs = 42_000L, playWhenReady = true)
            bridge = ProviderAudioOnlyServiceBridge(player, repository)
        }

        try {
            val enabled = AtomicReference<Result<Unit>>()
            onMain { bridge.setAudioOnly(true, enabled::set) }
            waitForMain()

            assertTrue(requireNotNull(enabled.get()).isSuccess)
            assertTrue(onMain { requireNotNull(player.currentMediaItem).isProviderAudioOnly() })
            assertEquals(
                "https://instance.example/api/audio/first",
                onMain { player.currentMediaItem?.localConfiguration?.uri.toString() },
            )
            assertTrue(onMain { player.currentPosition } in 42_000L..43_000L)
            assertTrue(onMain { player.playWhenReady })
            assertEquals(listOf(EXPECTED_REQUEST), repository.requests)

            val disabled = AtomicReference<Result<Unit>>()
            onMain { bridge.setAudioOnly(false, disabled::set) }
            waitForMain()

            assertTrue(requireNotNull(disabled.get()).isSuccess)
            assertFalse(onMain { requireNotNull(player.currentMediaItem).isProviderAudioOnly() })
            assertEquals(
                original.localConfiguration?.uri,
                onMain { player.currentMediaItem?.localConfiguration?.uri },
            )
            assertTrue(onMain { player.currentPosition } in 42_000L..43_000L)
            assertTrue(onMain { player.playWhenReady })
            assertEquals(2, onMain { player.prepareCount })
        } finally {
            onMain {
                bridge.close()
                player.release()
            }
        }
    }

    @Test
    fun livePlaybackIsRejectedWithoutCallingTheProviderEndpoint() {
        val repository = RecordingAudioOnlyRepository(audioStream("unused"))
        val original = providerMediaItem(isLiveContent = true)
        lateinit var player: AudioOnlyTestPlayer
        lateinit var bridge: ProviderAudioOnlyServiceBridge
        onMain {
            player = AudioOnlyTestPlayer(original)
            bridge = ProviderAudioOnlyServiceBridge(player, repository)
        }

        try {
            val result = AtomicReference<Result<Unit>>()
            onMain { bridge.setAudioOnly(true, result::set) }

            assertTrue(requireNotNull(result.get()).exceptionOrNull() is AudioOnlyUnavailableFailure)
            assertTrue(repository.requests.isEmpty())
        } finally {
            onMain {
                bridge.close()
                player.release()
            }
        }
    }

    @Test
    fun anExpiredProviderUrlIsRenewedOnceWithinTheRetryWindow() {
        val repository = RecordingAudioOnlyRepository(audioStream("old"), audioStream("fresh"))
        lateinit var player: AudioOnlyTestPlayer
        lateinit var bridge: ProviderAudioOnlyServiceBridge
        var elapsedRealtime = 100L
        onMain {
            player = AudioOnlyTestPlayer(providerMediaItem(), positionMs = 8_000L)
            bridge = ProviderAudioOnlyServiceBridge(player, repository) { elapsedRealtime }
        }

        try {
            onMain { bridge.setAudioOnly(true) {} }
            waitForMain()
            val failure = expiredUrlFailure()

            onMain { bridge.onPlayerError(failure) }
            waitForMain()

            assertEquals(2, repository.requests.size)
            assertEquals(
                "https://instance.example/api/audio/fresh",
                onMain { player.currentMediaItem?.localConfiguration?.uri.toString() },
            )
            assertEquals(8_000L, onMain { player.currentPosition })

            elapsedRealtime += 1_000L
            onMain { bridge.onPlayerError(failure) }
            waitForMain()

            assertEquals(2, repository.requests.size)
        } finally {
            onMain {
                bridge.close()
                player.release()
            }
        }
    }

    private fun waitForMain() = InstrumentationRegistry.getInstrumentation().waitForIdleSync()

    private fun <T> onMain(block: () -> T): T {
        val result = AtomicReference<Result<T>>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            result.set(runCatching(block))
        }
        return requireNotNull(result.get()).getOrThrow()
    }
}

private class RecordingAudioOnlyRepository(
    vararg streams: AudioOnlyStream,
) : AudioOnlyStreamRepository {
    private val streams = streams.toMutableList()
    val requests = mutableListOf<ProviderAudioOnlyRequest>()

    override suspend fun resolve(
        requestScope: StreamRequestScope,
        videoUrl: String,
        preferOriginal: Boolean,
        preferredLocale: String,
    ): Result<AudioOnlyStream> {
        requests += ProviderAudioOnlyRequest(
            videoUrl,
            requestScope,
            preferOriginal,
            preferredLocale,
        )
        return Result.success(streams.removeAt(0))
    }
}

private class AudioOnlyTestPlayer(
    initialItem: MediaItem,
    positionMs: Long = 0L,
    playWhenReady: Boolean = false,
) : SimpleBasePlayer(Looper.getMainLooper()) {
    private var mediaItem = initialItem
    private var positionMs = positionMs
    private var playWhenReady = playWhenReady
    var prepareCount = 0
        private set

    override fun getState(): State = State.Builder()
        .setAvailableCommands(Player.Commands.Builder().addAllCommands().build())
        .setPlaybackState(Player.STATE_READY)
        .setPlayWhenReady(
            playWhenReady,
            Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
        )
        .setPlaylist(
            listOf(
                MediaItemData.Builder(MEDIA_UID)
                    .setMediaItem(mediaItem)
                    .build(),
            ),
        )
        .setContentPositionMs(positionMs)
        .build()

    override fun handleSetMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<*> {
        mediaItem = mediaItems[startIndex]
        positionMs = startPositionMs
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handlePrepare(): ListenableFuture<*> {
        prepareCount++
        return Futures.immediateVoidFuture()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        this.playWhenReady = playWhenReady
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleRelease(): ListenableFuture<*> = Futures.immediateVoidFuture()
}

private fun providerMediaItem(isLiveContent: Boolean = false): MediaItem {
    val extras = Bundle().apply {
        putString(MergedStreamMediaKeys.EXTRA_SERVER_ID, TEST_SCOPE.serverId)
        putString(MergedStreamMediaKeys.EXTRA_ACCOUNT_ID, TEST_SCOPE.accountId)
        putString(MergedStreamMediaKeys.EXTRA_SERVER_BASE_URL, TEST_SCOPE.baseUrl)
        putString(MergedStreamMediaKeys.EXTRA_SOURCE_KEY, "provider:video")
        putBoolean(MergedStreamMediaKeys.EXTRA_AUDIO_ONLY_PREFER_ORIGINAL, true)
        putString(MergedStreamMediaKeys.EXTRA_AUDIO_ONLY_PREFERRED_LOCALE, "es")
        putBoolean(MergedStreamMediaKeys.EXTRA_IS_LIVE_CONTENT, isLiveContent)
    }
    return MediaItem.Builder()
        .setMediaId(VIDEO_URL)
        .setUri("https://instance.example/api/video")
        .setMimeType("video/mp4")
        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setExtras(extras).build())
        .build()
}

private fun audioStream(name: String) = AudioOnlyStream(
    url = "https://instance.example/api/audio/$name",
    kind = AudioOnlyStreamKind.Progressive,
    mimeType = "audio/mp4",
    codec = "mp4a.40.2",
    bitrate = 128_000,
    contentLength = 4_096,
    durationMillis = 60_000L,
)

private fun expiredUrlFailure(): PlaybackException {
    val dataSpec = DataSpec(Uri.parse("https://instance.example/api/audio/old"))
    val cause = HttpDataSource.InvalidResponseCodeException(
        401,
        "Expired",
        null,
        emptyMap(),
        dataSpec,
        byteArrayOf(),
    )
    return PlaybackException("Expired audio URL", cause, PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS)
}

private val TEST_SCOPE = StreamRequestScope(
    serverId = "server",
    accountId = "account",
    baseUrl = "https://instance.example/api/",
)
private val EXPECTED_REQUEST = ProviderAudioOnlyRequest(VIDEO_URL, TEST_SCOPE, true, "es")
private const val VIDEO_URL = "https://www.youtube.com/watch?v=video"
private const val MEDIA_UID = "audio-only-item"
