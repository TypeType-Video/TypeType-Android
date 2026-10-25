package dev.typetype.android.data.network

import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Singleton
class ApiBaseUrlHolder @Inject constructor(
    serverRepository: ServerRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    var currentEndpoint: CurrentServerEndpoint? = null
        private set

    private val endpointFlow = MutableStateFlow<CurrentServerEndpoint?>(null)

    val currentBaseUrl: String?
        get() = currentEndpoint?.baseUrl

    init {
        scope.launch {
            serverRepository.observeCurrentServer().collect { server ->
                currentEndpoint = server?.let(CurrentServerEndpoint::from)
                endpointFlow.value = currentEndpoint
            }
        }
    }

    suspend fun await(serverId: String): CurrentServerEndpoint =
        endpointFlow.filterNotNull().first { it.serverId == serverId }

    suspend fun awaitCurrent(): CurrentServerEndpoint = endpointFlow.filterNotNull().first()
}
