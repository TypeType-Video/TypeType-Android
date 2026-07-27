package dev.typetype.android.domain.auth

enum class SessionStatus { Valid, Invalid, Unknown }

interface AuthRepository {
    suspend fun loginWithCredentials(serverId: String, identifier: String, password: String): Result<Unit>
    suspend fun loginAsGuest(serverId: String): Result<Unit>
    suspend fun validateSession(): SessionStatus
    suspend fun logout(serverId: String): Result<Unit>
    suspend fun startOidc(serverId: String): Result<OidcAuthorization>
    suspend fun finishOidc(serverId: String, callbackUrl: String): Result<Unit>
    suspend fun cancelOidc(serverId: String)
}

data class OidcAuthorization(
    val authorizationUrl: String,
    val redirectScheme: String,
)
