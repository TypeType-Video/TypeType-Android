package dev.typetype.android.data.library.sync

import androidx.work.Data

internal object ProgressSyncWorkContract {
    const val SERVER_ID = "server_id"
    const val ACCOUNT_ID = "account_id"
    const val BASE_URL = "base_url"
    const val SESSION_GENERATION = "session_generation"

    fun input(serverId: String, accountId: String, baseUrl: String, generation: Long): Data =
        Data.Builder()
            .putString(SERVER_ID, serverId)
            .putString(ACCOUNT_ID, accountId)
            .putString(BASE_URL, baseUrl)
            .putLong(SESSION_GENERATION, generation)
            .build()
}
