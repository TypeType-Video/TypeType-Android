package dev.typetype.android.data.imports

import androidx.work.Data
import androidx.work.workDataOf

internal object YoutubeTakeoutWorkContract {
    const val SERVER_ID = "server_id"
    const val ACCOUNT_ID = "account_id"
    const val BASE_URL = "base_url"
    const val SESSION_GENERATION = "session_generation"

    fun input(serverId: String, accountId: String, baseUrl: String, generation: Long): Data =
        workDataOf(
            SERVER_ID to serverId,
            ACCOUNT_ID to accountId,
            BASE_URL to baseUrl,
            SESSION_GENERATION to generation,
        )
}
