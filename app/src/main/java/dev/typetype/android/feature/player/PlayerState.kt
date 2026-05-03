package dev.typetype.android.feature.player

import dev.typetype.android.domain.stream.Stream

data class PlayerState(
    val isLoading: Boolean = true,
    val stream: Stream? = null,
    val errorMessage: String? = null,
)
