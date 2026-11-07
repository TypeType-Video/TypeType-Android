package dev.typetype.android.feature.settings.player

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.domain.usersettings.SEEKBAR_ICON_MAX_BYTES
import dev.typetype.android.domain.usersettings.SeekbarIconUpload
import dev.typetype.android.feature.settings.profile.detectAvatarMediaType
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject

sealed interface SeekbarIconSelection {
    class Ready(val upload: SeekbarIconUpload) : SeekbarIconSelection
    data object TooLarge : SeekbarIconSelection
    data object Unsupported : SeekbarIconSelection
}

class SeekbarIconSelectionReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun read(uri: Uri): SeekbarIconSelection {
        val input = context.contentResolver.openInputStream(uri) ?: return SeekbarIconSelection.Unsupported
        val bytes = input.use { readSeekbarIcon(it) } ?: return SeekbarIconSelection.TooLarge
        val mediaType = detectAvatarMediaType(bytes) ?: return SeekbarIconSelection.Unsupported
        return SeekbarIconSelection.Ready(SeekbarIconUpload(bytes, mediaType))
    }
}

internal fun readSeekbarIcon(input: InputStream, maxBytes: Int = SEEKBAR_ICON_MAX_BYTES): ByteArray? {
    val output = ByteArrayOutputStream(minOf(maxBytes, DEFAULT_BUFFER_SIZE))
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val count = input.read(buffer)
        if (count < 0) return output.toByteArray()
        total += count
        if (total > maxBytes) return null
        output.write(buffer, 0, count)
    }
}
