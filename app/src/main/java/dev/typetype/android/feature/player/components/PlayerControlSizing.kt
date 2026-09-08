package dev.typetype.android.feature.player.components

internal fun useExpandedPlayerControls(
    smallestWindowWidthDp: Int,
    playerWidthDp: Float,
    playerHeightDp: Float,
): Boolean = smallestWindowWidthDp >= 600 && playerWidthDp >= 600 && playerHeightDp >= 300
