package dev.typetype.android.core.ui.util

fun Long.knownCountOrNull(): Long? = takeIf { it >= 0L }
