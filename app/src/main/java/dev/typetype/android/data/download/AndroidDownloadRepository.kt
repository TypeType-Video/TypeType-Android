package dev.typetype.android.data.download

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.account.AccountDao
import dev.typetype.android.data.network.AccessTokenStore
import dev.typetype.android.data.network.DownloaderGatewayApi
import dev.typetype.android.data.network.ScopedApiFactory
import dev.typetype.android.data.network.extractServerError
import dev.typetype.android.domain.download.DownloadFailure
import dev.typetype.android.domain.download.DownloadItem
import dev.typetype.android.domain.download.DownloadProgress
import dev.typetype.android.domain.download.DownloadRepository
import dev.typetype.android.domain.download.DownloadSelection
import dev.typetype.android.domain.download.DownloadStage
import dev.typetype.android.domain.download.DownloadStatus
import dev.typetype.android.domain.server.ServerRepository
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class AndroidDownloadRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val activeAccountScope: ActiveAccountScope,
    private val accountDao: AccountDao,
    private val serverRepository: ServerRepository,
    private val downloadDao: DownloadDao,
    private val artifactManager: DownloadArtifactManager,
    private val scopedApiFactory: ScopedApiFactory,
    private val tokenStore: AccessTokenStore,
) : DownloadRepository {
    private val workManager = WorkManager.getInstance(context)
    private val refreshes = MutableStateFlow(0L)

    override fun observeDownloads(): Flow<List<DownloadItem>> = activeAccountScope.observe()
        .flatMapLatest { scope ->
            if (scope == null) {
                flowOf(emptyList())
            } else {
                combine(downloadDao.observeAll(scope.serverId, scope.accountId), refreshes) { entries, _ ->
                    entries.map { it.toItem() }
                }
            }
        }
        .flowOn(Dispatchers.IO)

    override fun refreshDownloads() {
        refreshes.update { it + 1L }
    }

    override fun downloadVideo(
        videoUrl: String,
        title: String,
        selection: DownloadSelection,
    ): Flow<DownloadProgress> = flow {
        val scope = activeAccountScope.require()
        val baseUrl = requireServerBaseUrl(scope)
        val generation = requireAccountGeneration(scope)
        val requestId = UUID.randomUUID().toString()
        val request = createWorkRequest(scope, baseUrl, generation)
        val now = System.currentTimeMillis()
        downloadDao.upsert(
            DownloadEntity(
                serverId = scope.serverId,
                accountId = scope.accountId,
                sessionGeneration = generation,
                requestId = requestId,
                workId = request.id.toString(),
                videoUrl = videoUrl,
                title = title.ifBlank { DEFAULT_VIDEO_NAME },
                quality = selection.storageKey,
                serverJobId = null,
                systemDownloadId = null,
                fileName = null,
                status = STATUS_QUEUED,
                progressPercent = null,
                stage = null,
                errorMessage = null,
                cached = false,
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
        workManager.enqueueUniqueWork(workName(requestId), ExistingWorkPolicy.KEEP, request)
        emit(DownloadProgress.Queued(cached = false))

        val terminal = downloadDao.observe(scope.serverId, scope.accountId, requestId)
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { entry ->
                when (entry.status) {
                    STATUS_QUEUED -> emit(DownloadProgress.Queued(entry.cached))
                    STATUS_RUNNING -> emit(DownloadProgress.Running(entry.progressPercent, entry.stage))
                }
            }
            .first { it.status == STATUS_ENQUEUED || it.status == STATUS_FAILED }
        activeAccountScope.verify(scope)
        if (terminal.status == STATUS_FAILED) error(terminal.errorMessage ?: "Download failed")
        emit(
            DownloadProgress.Enqueued(
                downloadId = requireNotNull(terminal.systemDownloadId),
                fileName = terminal.fileName ?: terminal.title,
            ),
        )
    }.flowOn(Dispatchers.IO)

    override suspend fun openDownload(requestId: String): Result<Unit> = runCatching {
        val scope = activeAccountScope.require()
        val entry = downloadDao.get(scope.serverId, scope.accountId, requestId)
            ?: error("Download not found")
        artifactManager.open(requireNotNull(entry.systemDownloadId) { "Download is not ready" })
    }

    override suspend fun cancelDownload(requestId: String): Result<Unit> = runCatching {
        val scope = activeAccountScope.require()
        val entry = requireEntry(scope, requestId)
        if (entry.serverJobId != null && entry.status != STATUS_ENQUEUED) {
            cancelServerJob(scope, entry.serverJobId)
        }
        workManager.cancelUniqueWork(workName(requestId))
        val latest = requireEntry(scope, requestId)
        latest.systemDownloadId?.let(artifactManager::remove)
        downloadDao.updateProgress(
            latest.workId,
            STATUS_CANCELLED,
            null,
            null,
            DownloadFailureCodes.Cancelled,
            System.currentTimeMillis(),
        )
    }

    override suspend fun retryDownload(requestId: String): Result<Unit> = runCatching {
        val scope = activeAccountScope.require()
        val entry = requireEntry(scope, requestId)
        require(entry.status == STATUS_FAILED || entry.status == STATUS_CANCELLED) { "Download is active" }
        val baseUrl = requireServerBaseUrl(scope)
        val generation = requireAccountGeneration(scope)
        val request = createWorkRequest(scope, baseUrl, generation)
        entry.systemDownloadId?.let(artifactManager::remove)
        downloadDao.upsert(
            entry.copy(
                workId = request.id.toString(),
                sessionGeneration = generation,
                serverJobId = null,
                systemDownloadId = null,
                fileName = null,
                status = STATUS_QUEUED,
                progressPercent = null,
                stage = null,
                errorMessage = null,
                cached = false,
                updatedAtMillis = System.currentTimeMillis(),
            ),
        )
        workManager.enqueueUniqueWork(workName(requestId), ExistingWorkPolicy.REPLACE, request)
    }

    override suspend fun removeDownload(requestId: String): Result<Unit> = runCatching {
        val scope = activeAccountScope.require()
        val entry = requireEntry(scope, requestId)
        require(entry.status != STATUS_RUNNING && entry.status != STATUS_QUEUED) { "Cancel the download first" }
        workManager.cancelUniqueWork(workName(requestId))
        entry.systemDownloadId?.let(artifactManager::remove)
        downloadDao.delete(scope.serverId, scope.accountId, requestId)
    }

    private suspend fun cancelServerJob(scope: AccountScope, jobId: String) {
        val baseUrl = requireServerBaseUrl(scope)
        val token = tokenStore.getAccessToken(scope.serverId, scope.accountId)
            ?: error(DownloadFailureCodes.Authentication)
        val api = scopedApiFactory.create(
            baseUrl = baseUrl,
            serverId = scope.serverId,
            accountId = scope.accountId,
            token = token,
            type = DownloaderGatewayApi::class.java,
        )
        val response = api.cancelJob(jobId, "Bearer $token")
        if (!response.isSuccessful && response.code() != 404) {
            error(DownloadFailureCodes.fromHttp(response.code(), extractServerError(response)))
        }
    }

    private suspend fun requireEntry(scope: AccountScope, requestId: String): DownloadEntity =
        downloadDao.get(scope.serverId, scope.accountId, requestId) ?: error("Download not found")

    private suspend fun requireAccountGeneration(scope: AccountScope): Long =
        accountDao.get(scope.serverId, scope.accountId)?.sessionGeneration
            ?: error("Account not found")

    private suspend fun requireServerBaseUrl(scope: AccountScope): String =
        serverRepository.getServer(scope.serverId)?.baseUrl ?: error("Instance not found")

    private fun createWorkRequest(scope: AccountScope, baseUrl: String, generation: Long) =
        OneTimeWorkRequestBuilder<DownloadPreparationWorker>()
            .setInputData(DownloadWorkContract.input(scope.serverId, scope.accountId, baseUrl, generation))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, MIN_BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()

    private fun DownloadEntity.toItem(): DownloadItem {
        val artifactStatus = systemDownloadId?.let(artifactManager::status)
        val mappedStatus = when (status) {
            STATUS_RUNNING -> DownloadStatus.Running to progressPercent
            STATUS_ENQUEUED -> artifactStatus ?: (DownloadStatus.Pending to null)
            STATUS_FAILED -> DownloadStatus.Failed to null
            STATUS_CANCELLED -> DownloadStatus.Cancelled to null
            else -> DownloadStatus.Pending to progressPercent
        }
        return DownloadItem(
            requestId = requestId,
            systemDownloadId = systemDownloadId,
            title = title,
            fileName = fileName ?: title,
            selection = DownloadSelection.fromStorage(quality),
            status = mappedStatus.first,
            progressPercent = mappedStatus.second,
            stage = stage.toDownloadStage(),
            failure = when {
                mappedStatus.first != DownloadStatus.Failed -> null
                else -> errorMessage.toDownloadFailure()
            },
            createdAtMillis = createdAtMillis,
        )
    }

    private fun String?.toDownloadStage(): DownloadStage? = when (this) {
        "download", "downloading" -> DownloadStage.Downloading
        "mux", "finalizing" -> DownloadStage.Finalizing
        "extract", "running" -> DownloadStage.Preparing
        else -> null
    }

    private fun String?.toDownloadFailure(): DownloadFailure = when (this) {
        DownloadFailureCodes.Authentication -> DownloadFailure.Authentication
        DownloadFailureCodes.InsufficientStorage -> DownloadFailure.InsufficientStorage
        DownloadFailureCodes.Network -> DownloadFailure.Network
        DownloadFailureCodes.Rejected -> DownloadFailure.Rejected
        DownloadFailureCodes.ServerUnavailable -> DownloadFailure.ServerUnavailable
        DownloadFailureCodes.TimedOut -> DownloadFailure.TimedOut
        else -> DownloadFailure.Unknown
    }

    private fun workName(requestId: String) = "typetype-download-$requestId"

    private companion object {
        const val DEFAULT_VIDEO_NAME = "TypeType video"
        const val MIN_BACKOFF_SECONDS = 10L
        const val STATUS_QUEUED = "QUEUED"
        const val STATUS_RUNNING = "RUNNING"
        const val STATUS_ENQUEUED = "ENQUEUED"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_CANCELLED = "CANCELLED"
    }
}
