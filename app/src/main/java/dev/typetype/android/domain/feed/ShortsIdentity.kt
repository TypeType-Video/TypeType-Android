package dev.typetype.android.domain.feed

import dev.typetype.android.domain.navigation.resolveIncomingVideoUrl

fun Video.shortIdentity(): String = resolveIncomingVideoUrl(url) ?: url.ifBlank { id }
