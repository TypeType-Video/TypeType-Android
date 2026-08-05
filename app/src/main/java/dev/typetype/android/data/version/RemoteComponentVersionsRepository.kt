package dev.typetype.android.data.version

import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.ComponentVersionDto
import dev.typetype.android.domain.version.ComponentVersion
import dev.typetype.android.domain.version.ComponentVersions
import dev.typetype.android.domain.version.ComponentVersionsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import retrofit2.Response

@Singleton
class RemoteComponentVersionsRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
) : ComponentVersionsRepository {
    override suspend fun fetch(): Result<ComponentVersions> = captureVersionResult {
        val scope = activeAccountScope.require()
        val api = apiHolder.require(scope)
        val versions = supervisorScope {
            val frontend = async { loadComponentVersion(api::frontendVersion) }
            val server = async { loadComponentVersion(api::serverVersion) }
            val token = async { loadComponentVersion(api::tokenVersion) }
            val downloader = async { loadComponentVersion(api::downloaderVersion) }
            ComponentVersions(
                frontend = frontend.await(),
                server = server.await(),
                token = token.await(),
                downloader = downloader.await(),
            )
        }
        activeAccountScope.verify(scope)
        versions
    }
}

internal suspend fun loadComponentVersion(
    request: suspend () -> Response<ComponentVersionDto>,
): ComponentVersion? = try {
    val response = request()
    if (response.isSuccessful) response.body()?.toDomain() else null
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Throwable) {
    null
}

private fun ComponentVersionDto.toDomain() = ComponentVersion(
    service = service,
    version = version,
    revision = revision,
    shortRevision = shortRevision,
    buildTime = buildTime,
)

private suspend fun <T> captureVersionResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Throwable) {
    Result.failure(failure)
}
