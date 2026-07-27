package dev.typetype.android.services

import dev.typetype.android.data.network.AccessTokenStore
import dev.typetype.android.data.network.ScopedHttpClientFactory
import dev.typetype.android.domain.stream.StreamRequestScope
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Singleton
class ScopedMediaClientFactory @Inject constructor(
    private val tokenStore: AccessTokenStore,
    private val scopedHttpClientFactory: ScopedHttpClientFactory,
) {
    fun create(scope: StreamRequestScope): OkHttpClient =
        scopedHttpClientFactory.create(
            baseUrl = scope.baseUrl,
            serverId = scope.serverId,
            accountId = scope.accountId,
            token = tokenStore.getAccessToken(scope.serverId, scope.accountId),
        )
}
