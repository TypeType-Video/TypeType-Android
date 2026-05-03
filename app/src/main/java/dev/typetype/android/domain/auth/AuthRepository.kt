package dev.typetype.android.domain.auth

interface AuthRepository {
    suspend fun loginWithCredentials(serverId: String, identifier: String, password: String): Result<Unit>
    suspend fun loginAsGuest(serverId: String): Result<Unit>
}
