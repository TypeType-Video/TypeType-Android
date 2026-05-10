package dev.typetype.android.data.network

import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
class ApiBaseUrlHolder @Inject constructor(
    serverRepository: ServerRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    var currentBaseUrl: String? = null
        private set

    init {
        scope.launch {
            serverRepository.observeCurrentServer().collect { server ->
                currentBaseUrl = server?.baseUrl
            }
        }
    }
}
