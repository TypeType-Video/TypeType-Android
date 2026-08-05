package dev.typetype.android.feature.player

import dev.typetype.android.domain.comments.BulletComment

data class PlayerDanmakuState(
    val supported: Boolean = false,
    val available: Boolean = false,
    val enabled: Boolean = false,
    val speed: Float = 1f,
    val size: Float = 1f,
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
    val comments: List<BulletComment> = emptyList(),
)

sealed interface PlayerDanmakuAction {
    data class SetEnabled(val enabled: Boolean) : PlayerDanmakuAction
    data class SetSpeed(val speed: Float) : PlayerDanmakuAction
    data class SetSize(val size: Float) : PlayerDanmakuAction
}
