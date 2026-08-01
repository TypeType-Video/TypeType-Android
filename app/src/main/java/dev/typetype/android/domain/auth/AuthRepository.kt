package dev.typetype.android.domain.auth

enum class SessionStatus { Valid, Invalid, Unknown }

interface AuthRepository {
    suspend fun loginWithCredentials(serverId: String, identifier: String, password: String): Result<Unit>
    suspend fun loginAsGuest(serverId: String): Result<Unit>
    suspend fun resetPassword(
        serverId: String,
        resetToken: String,
        newPassword: String,
    ): Result<Unit>
    suspend fun validateSession(): SessionStatus
    suspend fun logout(serverId: String): Result<Unit>
    suspend fun getLoginMethods(serverId: String): Result<LoginMethods>
    suspend fun startOidc(serverId: String): Result<OidcAuthorization>
    suspend fun finishOidc(serverId: String, callbackUrl: String): Result<Unit>
    suspend fun cancelOidc(serverId: String)
    fun hasPendingOidc(serverId: String): Boolean
}

data class LoginMethods(
    val localLoginEnabled: Boolean,
    val oidcEnabled: Boolean,
    val oidcProviderName: String?,
    val oidcAutoRedirect: Boolean,
)

data class OidcAuthorization(
    val authorizationUrl: String,
    val redirectScheme: String,
)
