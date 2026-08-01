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
            mediaType = detectAvatarMediaType(bytes) ?: error("AVATAR_FORMAT_UNSUPPORTED"),
        )
    }
}

internal fun supportedAvatarMimeTypes() = arrayOf(
    "image/jpeg",
    "image/png",
    "image/webp",
    "image/gif",
)

internal fun detectAvatarMediaType(bytes: ByteArray): String? = when {
    bytes.matches(0, 0xff, 0xd8, 0xff) -> "image/jpeg"
    bytes.matches(0, 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a) -> "image/png"
    bytes.matches(0, 0x47, 0x49, 0x46, 0x38, 0x37, 0x61) -> "image/gif"
    bytes.matches(0, 0x47, 0x49, 0x46, 0x38, 0x39, 0x61) -> "image/gif"
    bytes.matches(0, 0x52, 0x49, 0x46, 0x46) &&
        bytes.matches(8, 0x57, 0x45, 0x42, 0x50) -> "image/webp"
    else -> null
}

private fun ByteArray.matches(offset: Int, vararg expected: Int): Boolean =
    size >= offset + expected.size && expected.indices.all { index ->
        this[offset + index].toInt() and 0xff == expected[index]
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
