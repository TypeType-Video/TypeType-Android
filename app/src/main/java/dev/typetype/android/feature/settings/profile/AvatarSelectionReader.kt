package dev.typetype.android.feature.settings.profile

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.domain.profile.AvatarUpload
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject

class AvatarSelectionReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun read(uri: Uri): AvatarUpload {
        val input = context.contentResolver.openInputStream(uri)
            ?: error("AVATAR_FILE_UNAVAILABLE")
        val bytes = input.use { readBoundedAvatar(it) }
        if (bytes.isEmpty()) error("AVATAR_FILE_EMPTY")
        return AvatarUpload(
            bytes = bytes,
            mediaType = context.contentResolver.getType(uri) ?: "application/octet-stream",
        )
    }
}

internal fun readBoundedAvatar(
    input: InputStream,
    maxBytes: Int = MAX_AVATAR_BYTES,
): ByteArray {
    val output = ByteArrayOutputStream(minOf(maxBytes, DEFAULT_BUFFER_SIZE))
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val count = input.read(buffer)
        if (count < 0) return output.toByteArray()
        total += count
        if (total > maxBytes) error("AVATAR_TOO_LARGE")
        output.write(buffer, 0, count)
    }
}

internal const val MAX_AVATAR_BYTES = 10 * 1024 * 1024
