package dev.typetype.android.data.network

interface ScopedAccessTokenStore {
    fun getAccessToken(serverId: String, accountId: String): String?
    fun setAccessToken(serverId: String, accountId: String, token: String?)
}
