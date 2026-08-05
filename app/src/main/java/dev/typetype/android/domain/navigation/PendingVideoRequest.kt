package dev.typetype.android.domain.navigation

internal class PendingVideoRequest {
    private var ready = false
    private var pendingUrl: String? = null
    private var revision = 0L

    val currentRevision: Long
        get() = revision

    fun submit(url: String): String? {
        revision += 1
        pendingUrl = url
        return takeIfReady()
    }

    fun setReady(value: Boolean): String? {
        ready = value
        return takeIfReady()
    }

    fun isCurrent(expectedRevision: Long): Boolean = revision == expectedRevision

    fun clear() {
        revision += 1
        ready = false
        pendingUrl = null
    }

    private fun takeIfReady(): String? {
        if (!ready) return null
        return pendingUrl.also { pendingUrl = null }
    }
}
