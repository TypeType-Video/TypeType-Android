package dev.typetype.android.domain.usersettings

interface SeekbarIconRepository {
    suspend fun upload(icon: SeekbarIconUpload): Result<Unit>
    suspend fun delete(): Result<Unit>
}

class SeekbarIconUpload(
    val bytes: ByteArray,
    val mediaType: String,
)

const val SEEKBAR_ICON_MAX_BYTES = 2 * 1024 * 1024
val SEEKBAR_ICON_MEDIA_TYPES = setOf("image/png", "image/jpeg", "image/gif", "image/webp")
