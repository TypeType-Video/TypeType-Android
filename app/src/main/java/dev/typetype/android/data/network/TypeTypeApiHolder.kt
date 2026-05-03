package dev.typetype.android.data.network

import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Singleton
class TypeTypeApiHolder @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
    serverRepository: ServerRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _api = MutableStateFlow<TypeTypeApi?>(null)
    val api: StateFlow<TypeTypeApi?> = _api.asStateFlow()

    init {
        scope.launch {
            serverRepository.observeCurrentServer().collect { server ->
                _api.value = server?.let { retrofitFactory.create(it.baseUrl) }
            }
        }
    }

    suspend fun require(): TypeTypeApi =
        api.value ?: error("No server is currently selected")
}
