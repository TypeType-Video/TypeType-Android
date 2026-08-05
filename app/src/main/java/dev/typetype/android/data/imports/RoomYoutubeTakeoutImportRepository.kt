package dev.typetype.android.data.imports

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.data.account.AccountDao
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.domain.imports.ImportDocument
import dev.typetype.android.domain.imports.YoutubeTakeoutCategoryCounts
import dev.typetype.android.domain.imports.YoutubeTakeoutImportItem
import dev.typetype.android.domain.imports.YoutubeTakeoutImportRepository
import dev.typetype.android.domain.imports.YoutubeTakeoutImportStatus
import dev.typetype.android.domain.server.ServerRepository
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class RoomYoutubeTakeoutImportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activeAccountScope: ActiveAccountScope,
    private val accountDao: AccountDao,
    private val serverRepository: ServerRepository,
    private val importDao: YoutubeTakeoutImportDao,
) : YoutubeTakeoutImportRepository {
    private val workManager = WorkManager.getInstance(context)

    override fun observeImports(): Flow<List<YoutubeTakeoutImportItem>> = activeAccountScope.observe()
        .flatMapLatest { scope ->
            if (scope == null) flowOf(emptyList())
            else importDao.observeAll(scope.serverId, scope.accountId).map { entries -> entries.map { it.toItem() } }
        }
        .flowOn(Dispatchers.IO)

    override suspend fun enqueue(documents: List<ImportDocument>): Result<Int> = captureResult {
        require(documents.isNotEmpty()) { YoutubeTakeoutFailureCodes.InvalidArchive }
        val scope = activeAccountScope.require()
        val account = accountDao.get(scope.serverId, scope.accountId)
            ?: error(YoutubeTakeoutFailureCodes.Authentication)
        require(!account.isGuest) { YoutubeTakeoutFailureCodes.AccountRequired }
        val baseUrl = serverRepository.getServer(scope.serverId)?.baseUrl
            ?: error(YoutubeTakeoutFailureCodes.ServerUnavailable)
        val generation = account.sessionGeneration
        documents.forEach(::validate)

        documents.forEach { document ->
            retainPermission(document.uri)
            val requestId = UUID.randomUUID().toString()
            val request = createWorkRequest(scope, baseUrl, generation)
            val now = System.currentTimeMillis()
            try {
                importDao.upsert(document.toEntity(scope, generation, requestId, request.id.toString(), now))
                workManager.enqueueUniqueWork(
                    queueName(scope, generation),
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    request,
                )
            } catch (failure: Throwable) {
                importDao.delete(scope.serverId, scope.accountId, requestId)
                releasePermission(document.uri)
                throw failure
            }
        }
        documents.size
    }

    override suspend fun retry(requestId: String): Result<Unit> = captureResult {
        val scope = activeAccountScope.require()
        val entry = requireEntry(scope, requestId)
        require(entry.status == "FAILED" || entry.status == "CANCELLED") { "YOUTUBE_IMPORT_ACTIVE" }
        val account = accountDao.get(scope.serverId, scope.accountId)
            ?: error(YoutubeTakeoutFailureCodes.Authentication)
        val baseUrl = serverRepository.getServer(scope.serverId)?.baseUrl
            ?: error(YoutubeTakeoutFailureCodes.ServerUnavailable)
        retainPermission(entry.documentUri)
        val request = createWorkRequest(scope, baseUrl, account.sessionGeneration)
        importDao.upsert(
            entry.copy(
                sessionGeneration = account.sessionGeneration,
                workId = request.id.toString(),
                serverJobId = entry.serverJobId.takeUnless { entry.failureCode.requiresFreshUpload() },
                status = "QUEUED",
                phase = null,
                progressPercent = null,
                failureCode = null,
                failureRequestId = null,
                updatedAtMillis = System.currentTimeMillis(),
            ),
        )
        workManager.enqueueUniqueWork(
            queueName(scope, account.sessionGeneration),
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
    }

    override suspend fun cancel(requestId: String): Result<Unit> = captureResult {
        val scope = activeAccountScope.require()
        val entry = requireEntry(scope, requestId)
        require(entry.status !in TERMINAL_STATUSES) { "YOUTUBE_IMPORT_NOT_ACTIVE" }
        val updated = importDao.cancel(
            entry.workId,
            YoutubeTakeoutFailureCodes.Cancelled,
            System.currentTimeMillis(),
        )
        require(updated > 0) { "YOUTUBE_IMPORT_NOT_ACTIVE" }
        workManager.cancelWorkById(UUID.fromString(entry.workId))
    }

    override suspend fun remove(requestId: String): Result<Unit> = captureResult {
        val scope = activeAccountScope.require()
        val entry = requireEntry(scope, requestId)
        require(entry.status in TERMINAL_STATUSES) { "YOUTUBE_IMPORT_ACTIVE" }
        workManager.cancelWorkById(UUID.fromString(entry.workId))
        importDao.delete(scope.serverId, scope.accountId, requestId)
        releasePermission(entry.documentUri)
    }

    override suspend fun acknowledgeCollectionRefresh(requestId: String): Result<Unit> = captureResult {
        val scope = activeAccountScope.require()
        requireEntry(scope, requestId)
        importDao.markCollectionsRefreshed(
            serverId = scope.serverId,
            accountId = scope.accountId,
            requestId = requestId,
            updatedAt = System.currentTimeMillis(),
        )
    }

    private suspend fun requireEntry(scope: AccountScope, requestId: String): YoutubeTakeoutImportEntity =
        importDao.get(scope.serverId, scope.accountId, requestId)
            ?: error("YOUTUBE_IMPORT_NOT_FOUND")

    private fun createWorkRequest(
        scope: AccountScope,
        baseUrl: String,
        generation: Long,
    ): OneTimeWorkRequest = OneTimeWorkRequestBuilder<YoutubeTakeoutImportWorker>()
        .setInputData(YoutubeTakeoutWorkContract.input(scope.serverId, scope.accountId, baseUrl, generation))
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, MIN_BACKOFF_SECONDS, TimeUnit.SECONDS)
        .build()

    private fun validate(document: ImportDocument) {
        require(document.displayName.endsWith(".zip", ignoreCase = true)) {
            YoutubeTakeoutFailureCodes.InvalidArchive
        }
        document.sizeBytes?.let { size ->
            require(size > 0L) { YoutubeTakeoutFailureCodes.InvalidArchive }
            require(size <= MAX_ARCHIVE_BYTES) { YoutubeTakeoutFailureCodes.TooLarge }
        }
    }

    private fun retainPermission(uri: String) {
        try {
            context.contentResolver.takePersistableUriPermission(
                Uri.parse(uri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (failure: SecurityException) {
            throw IllegalStateException(YoutubeTakeoutFailureCodes.Permission, failure)
        }
    }

    private fun releasePermission(uri: String) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(uri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    private fun ImportDocument.toEntity(
        scope: AccountScope,
        generation: Long,
        requestId: String,
        workId: String,
        now: Long,
    ) = YoutubeTakeoutImportEntity(
        serverId = scope.serverId,
        accountId = scope.accountId,
        sessionGeneration = generation,
        requestId = requestId,
        workId = workId,
        documentUri = uri,
        displayName = displayName,
        sizeBytes = sizeBytes,
        serverJobId = null,
        status = "QUEUED",
        phase = null,
        progressPercent = null,
        previewSubscriptions = null,
        previewPlaylists = null,
        previewPlaylistItems = null,
        previewFavorites = null,
        previewWatchLater = null,
        previewHistory = null,
        importedCount = null,
        skippedCount = null,
        failedCount = null,
        warningCount = 0,
        errorCount = 0,
        failureCode = null,
        failureRequestId = null,
        collectionsRefreshed = false,
        createdAtMillis = now,
        updatedAtMillis = now,
    )

    private fun YoutubeTakeoutImportEntity.toItem() = YoutubeTakeoutImportItem(
        requestId = requestId,
        displayName = displayName,
        sizeBytes = sizeBytes,
        status = status.toDomainStatus(),
        progressPercent = progressPercent,
        preview = previewCounts(),
        importedCount = importedCount,
        skippedCount = skippedCount,
        failedCount = failedCount,
        warningCount = warningCount,
        errorCount = errorCount,
        failureCode = failureCode,
        failureRequestId = failureRequestId,
        needsCollectionRefresh = status == "COMPLETED" && !collectionsRefreshed,
        createdAtMillis = createdAtMillis,
    )

    private fun YoutubeTakeoutImportEntity.previewCounts(): YoutubeTakeoutCategoryCounts? {
        val subscriptions = previewSubscriptions ?: return null
        return YoutubeTakeoutCategoryCounts(
            subscriptions = subscriptions,
            playlists = previewPlaylists ?: 0,
            playlistItems = previewPlaylistItems ?: 0,
            favorites = previewFavorites ?: 0,
            watchLater = previewWatchLater ?: 0,
            history = previewHistory ?: 0,
        )
    }

    private fun String.toDomainStatus(): YoutubeTakeoutImportStatus = when (this) {
        "UPLOADING" -> YoutubeTakeoutImportStatus.Uploading
        "PARSING" -> YoutubeTakeoutImportStatus.Parsing
        "IMPORTING" -> YoutubeTakeoutImportStatus.Importing
        "COMPLETED" -> YoutubeTakeoutImportStatus.Completed
        "FAILED" -> YoutubeTakeoutImportStatus.Failed
        "CANCELLED" -> YoutubeTakeoutImportStatus.Cancelled
        else -> YoutubeTakeoutImportStatus.Queued
    }

    private fun String?.requiresFreshUpload(): Boolean = this == YoutubeTakeoutFailureCodes.JobFailed ||
        this == YoutubeTakeoutFailureCodes.JobNotFound || this == YoutubeTakeoutFailureCodes.InvalidArchive

    private fun queueName(scope: AccountScope, generation: Long): String =
        "typetype-youtube-import-${scope.serverId}-${scope.accountId}-$generation"

    private suspend fun <T> captureResult(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Throwable) {
        Result.failure(failure)
    }

    private companion object {
        val TERMINAL_STATUSES = setOf("COMPLETED", "FAILED", "CANCELLED")
        const val MAX_ARCHIVE_BYTES = 2L * 1024L * 1024L * 1024L
        const val MIN_BACKOFF_SECONDS = 10L
    }
}
