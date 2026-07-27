package dev.typetype.android.data.subscriptions

import dev.typetype.android.domain.subscriptions.canonicalChannelUrl

internal fun normalizeChannelUrl(value: String): String = canonicalChannelUrl(value)
