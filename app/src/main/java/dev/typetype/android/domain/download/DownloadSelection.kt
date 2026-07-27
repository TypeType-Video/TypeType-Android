package dev.typetype.android.domain.download

data class DownloadSelection(
    val mode: DownloadMediaMode,
    val maxHeight: Int? = null,
) {
    init {
        require(mode == DownloadMediaMode.Audio || maxHeight != null)
        require(maxHeight == null || maxHeight > 0)
    }

    val storageKey: String
        get() = when (mode) {
            DownloadMediaMode.Audio -> AUDIO_STORAGE_KEY
            DownloadMediaMode.Video -> "video:$maxHeight"
        }

    companion object {
        val VideoHeights = listOf(360, 480, 720, 1080)
        val Default = video(1080)
        val Audio = DownloadSelection(DownloadMediaMode.Audio)

        fun video(maxHeight: Int) = DownloadSelection(
            mode = DownloadMediaMode.Video,
            maxHeight = maxHeight,
        )

        fun fromStorage(value: String): DownloadSelection {
            val normalized = value.trim().lowercase()
            if (normalized == AUDIO_STORAGE_KEY) return Audio
            val height = normalized
                .removePrefix("video:")
                .removeSuffix("p")
                .toIntOrNull()
                ?.takeIf { it > 0 }
            return height?.let(::video) ?: Default
        }

        private const val AUDIO_STORAGE_KEY = "audio"
    }
}

enum class DownloadMediaMode {
    Video,
    Audio,
}
