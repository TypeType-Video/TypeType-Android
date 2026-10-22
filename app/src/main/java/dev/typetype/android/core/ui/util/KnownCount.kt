package dev.typetype.android.core.ui.util

internal fun Long.knownCountOrNull(): Long? = takeIf { it >= 0L }
