package dev.typetype.android.data.imports

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.typetype.android.data.account.AccountDao
import dev.typetype.android.data.network.AccessTokenStore
import dev.typetype.android.data.network.ScopedApiFactory
import dev.typetype.android.data.network.TypeTypeImportApi
import dev.typetype.android.data.network.dto.YoutubeTakeoutCommitRequestDto
import dev.typetype.android.data.network.dto.YoutubeTakeoutImportStatsDto
import dev.typetype.android.data.network.dto.YoutubeTakeoutJobStatusDto
import dev.typetype.android.data.network.dto.YoutubeTakeoutReportDto
import dev.typetype.android.data.network.extractServerError
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import retrofit2.Response

@HiltWorker
class YoutubeTakeoutImportWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val importDao: YoutubeTakeoutImportDao,
    private val accountDao: AccountDao,
    private val scopedApiFactory: ScopedApiFactory,
    private val tokenStore: AccessTokenStore,
) : CoroutineWorker(context, parameters) {
    private val foregroundInfoFactory = YoutubeTakeoutForegroundInfoFactory(context)

    override suspend fun doWork(): Result {
        val entry = importDao.getByWorkId(id.toString()) ?: return Result.failure()
        if (entry.status == "CANCELLED" || entry.status == "COMPLETED") return Result.success()
        if (entry.status == "FAILED") return Result.failure()
        setForeground(foregroundInfoFactory.create(id, entry.displayName, entry.status, entry.progressPercent))
        val scope = readScope() ?: return fail(YoutubeTakeoutFailureCodes.ScopeChanged)
        val account = accountDao.get(scope.serverId, scope.accountId)
        if (
            entry.serverId != scope.serverId || entry.accountId != scope.accountId ||
            entry.sessionGeneration != scope.generation || account?.sessionGeneration != scope.generation
        ) {
            return fail(YoutubeTakeoutFailureCodes.ScopeChanged)
        }
        val token = tokenStore.getAccessToken(scope.serverId, scope.accountId)
            ?: return fail(YoutubeTakeoutFailureCodes.Authentication)
        val api = scopedApiFactory.createYoutubeTakeoutImport(
            baseUrl = scope.baseUrl,
            serverId = scope.serverId,
            accountId = scope.accountId,
            token = token,
            type = TypeTypeImportApi::class.java,
        )

        return try {
            val jobId = entry.serverJobId ?: upload(entry, api)
            resumeJob(jobId, api)
        } catch (failure: ImportFailure) {
            if (failure.retryable && runAttemptCount < MAX_ATTEMPTS) {
                retry(failure.code, failure.requestId)
            } else {
                fail(failure.code, failure.requestId)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            if (runAttemptCount < MAX_ATTEMPTS) retry(YoutubeTakeoutFailureCodes.Network)
            else fail(YoutubeTakeoutFailureCodes.Network)
        } catch (_: Exception) {
            fail(YoutubeTakeoutFailureCodes.Unknown)
        }
    }

    private suspend fun upload(entry: YoutubeTakeoutImportEntity, api: TypeTypeImportApi): String {
        update("UPLOADING", "uploading", null)
        val body = ContentUriRequestBody(
            contentResolver = applicationContext.contentResolver,
            uri = Uri.parse(entry.documentUri),
            mediaType = "application/zip".toMediaType(),
            knownSize = entry.sizeBytes,
            maxBytes = MAX_ARCHIVE_BYTES,
        )
        val response = api.uploadYoutubeTakeout(
            MultipartBody.Part.createFormData("archive", entry.displayName, body),
        )
        val created = response.requireBody(uploadRequest = true)
        val updated = importDao.attachServerJob(
            workId = id.toString(),
            jobId = created.jobId,
            phase = created.phase,
            progress = created.progress,
            updatedAt = System.currentTimeMillis(),
        )
        ensureActive(updated)
        return created.jobId
    }

    private suspend fun resumeJob(jobId: String, api: TypeTypeImportApi): Result {
        val status = api.youtubeTakeoutStatus(jobId).requireBody()
        return when (status.nextAction()) {
            YoutubeTakeoutJobAction.Complete -> complete(jobId, api)
            YoutubeTakeoutJobAction.Poll -> poll(jobId, api, status)
            YoutubeTakeoutJobAction.Preview -> previewAndCommit(jobId, api)
            YoutubeTakeoutJobAction.Fail -> throw ImportFailure(YoutubeTakeoutFailureCodes.JobFailed)
        }
    }

    private suspend fun previewAndCommit(jobId: String, api: TypeTypeImportApi): Result {
        update("PARSING", "parsing", 10)
        val preview = api.youtubeTakeoutPreview(jobId).requireBody()
        val issueCounts = preview.issueSummary.visibleCounts(preview.warnings.size, preview.errors.size)
        val updated = importDao.savePreview(
            workId = id.toString(),
            subscriptions = preview.counts.subscriptions,
            playlists = preview.counts.playlists,
            playlistItems = preview.counts.playlistItems,
            favorites = preview.counts.favorites,
            watchLater = preview.counts.watchLater,
            history = preview.counts.history,
            warnings = issueCounts.first,
            errors = issueCounts.second,
            updatedAt = System.currentTimeMillis(),
        )
        ensureActive(updated)
        val committed = api.commitYoutubeTakeout(jobId, ALL_CATEGORIES).requireBody()
        return poll(jobId, api, committed)
    }

    private suspend fun poll(
        jobId: String,
        api: TypeTypeImportApi,
        initial: YoutubeTakeoutJobStatusDto,
    ): Result {
        var status = initial
        repeat(POLLS_PER_RUN) {
            if (isStopped) return Result.retry()
            when (status.status) {
                "completed" -> return if (status.phase == "completed") {
                    complete(jobId, api)
                } else {
                    previewAndCommit(jobId, api)
                }
                "failed" -> throw ImportFailure(YoutubeTakeoutFailureCodes.JobFailed)
                else -> update("IMPORTING", status.phase, status.progress)
            }
            delay(POLL_DELAY_MILLIS)
            status = api.youtubeTakeoutStatus(jobId).requireBody()
        }
        throw ImportFailure(YoutubeTakeoutFailureCodes.TimedOut, retryable = true)
    }

    private suspend fun complete(jobId: String, api: TypeTypeImportApi): Result {
        val report = api.youtubeTakeoutReport(jobId).requireBody(reportRequest = true)
        val totals = report.buckets().totals()
        val issueCounts = report.issueSummary.visibleCounts(report.warnings.size, report.errors.size)
        val updated = importDao.complete(
            workId = id.toString(),
            imported = totals.imported,
            skipped = totals.skipped,
            failed = totals.failed,
            warnings = issueCounts.first,
            errors = issueCounts.second,
            updatedAt = System.currentTimeMillis(),
        )
        if (updated > 0) releaseDocumentPermission()
        return Result.success()
    }

    private suspend fun update(status: String, phase: String?, progress: Int?) {
        val updated = importDao.updateProgress(
            id.toString(),
            status,
            phase,
            progress,
            System.currentTimeMillis(),
        )
        ensureActive(updated)
        setForeground(foregroundInfoFactory.create(id, entryName(), status, progress))
    }

    private suspend fun retry(code: String, requestId: String? = null): Result {
        val updated = importDao.retrying(id.toString(), code, requestId, System.currentTimeMillis())
        return if (updated > 0) Result.retry() else Result.success()
    }

    private suspend fun fail(code: String, requestId: String? = null): Result {
        val updated = importDao.fail(id.toString(), code, requestId, System.currentTimeMillis())
        return if (updated > 0) Result.failure() else Result.success()
    }

    private fun ensureActive(updatedRows: Int) {
        if (updatedRows == 0) throw CancellationException(YoutubeTakeoutFailureCodes.Cancelled)
    }

    private suspend fun entryName(): String = importDao.getByWorkId(id.toString())?.displayName.orEmpty()

    private fun readScope(): WorkScope? {
        val serverId = inputData.getString(YoutubeTakeoutWorkContract.SERVER_ID) ?: return null
        val accountId = inputData.getString(YoutubeTakeoutWorkContract.ACCOUNT_ID) ?: return null
        val baseUrl = inputData.getString(YoutubeTakeoutWorkContract.BASE_URL) ?: return null
        val generation = inputData.getLong(YoutubeTakeoutWorkContract.SESSION_GENERATION, -1L)
        return generation.takeIf { it >= 0L }?.let { WorkScope(serverId, accountId, baseUrl, it) }
    }

    private suspend fun releaseDocumentPermission() {
        val entry = importDao.getByWorkId(id.toString()) ?: return
        runCatching {
            applicationContext.contentResolver.releasePersistableUriPermission(
                Uri.parse(entry.documentUri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    private fun <T> Response<T>.requireBody(
        uploadRequest: Boolean = false,
        reportRequest: Boolean = false,
    ): T {
        if (!isSuccessful) {
            val error = extractServerError(this)
            throw ImportFailure(
                code = YoutubeTakeoutFailureCodes.fromHttp(code(), error.code, uploadRequest),
                requestId = error.requestId,
                retryable = YoutubeTakeoutFailureCodes.isRetryable(code()) || (reportRequest && code() == 404),
            )
        }
        return body() ?: throw ImportFailure(YoutubeTakeoutFailureCodes.InvalidResponse)
    }

    private fun YoutubeTakeoutReportDto.buckets(): List<YoutubeTakeoutImportStatsDto> = listOf(
        subscriptions,
        playlists,
        playlistItems,
        favorites,
        watchLater,
        history,
    )

    private data class WorkScope(
        val serverId: String,
        val accountId: String,
        val baseUrl: String,
        val generation: Long,
    )

    private class ImportFailure(
        val code: String,
        val requestId: String? = null,
        val retryable: Boolean = false,
    ) : Exception(code)

    private companion object {
        const val MAX_ARCHIVE_BYTES = 2L * 1024L * 1024L * 1024L
        const val MAX_ATTEMPTS = 8
        const val POLLS_PER_RUN = 120
        const val POLL_DELAY_MILLIS = 2_500L
        val ALL_CATEGORIES = YoutubeTakeoutCommitRequestDto(
            importSubscriptions = true,
            importPlaylists = true,
            importPlaylistItems = true,
            importFavorites = true,
            importWatchLater = true,
            importHistory = true,
        )
    }
}
