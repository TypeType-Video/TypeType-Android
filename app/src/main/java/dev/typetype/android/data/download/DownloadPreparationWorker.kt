package dev.typetype.android.data.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.typetype.android.data.account.AccountDao
import dev.typetype.android.data.network.AccessTokenStore
import dev.typetype.android.data.network.DownloaderGatewayApi
import dev.typetype.android.data.network.ScopedApiFactory
import dev.typetype.android.data.network.dto.CreateDownloadJobRequest
import dev.typetype.android.data.network.dto.DownloadJobOptionsDto
import dev.typetype.android.data.network.dto.DownloadJobStatusDto
import dev.typetype.android.data.network.dto.DownloadModeDto
import dev.typetype.android.data.network.extractServerError
import dev.typetype.android.domain.download.DownloadMediaMode
import dev.typetype.android.domain.download.DownloadSelection
import java.io.IOException
import kotlinx.coroutines.delay

@HiltWorker
class DownloadPreparationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val downloadDao: DownloadDao,
    private val accountDao: AccountDao,
    private val scopedApiFactory: ScopedApiFactory,
    private val tokenStore: AccessTokenStore,
    private val artifactManager: DownloadArtifactManager,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val entry = downloadDao.getByWorkId(id.toString()) ?: return Result.failure()
        val serverId = inputData.getString(DownloadWorkContract.SERVER_ID)
            ?: return fail("Missing instance")
        val accountId = inputData.getString(DownloadWorkContract.ACCOUNT_ID)
            ?: return fail("Missing account")
        val baseUrl = inputData.getString(DownloadWorkContract.BASE_URL)
            ?: return fail("Missing address")
        val generation = inputData.getLong(DownloadWorkContract.SESSION_GENERATION, -1L)
        val account = accountDao.get(serverId, accountId)
        if (
            entry.serverId != serverId || entry.accountId != accountId ||
            generation < 0 || entry.sessionGeneration != generation ||
            account?.sessionGeneration != generation
        ) {
            return fail("Download scope changed")
        }
        val token = tokenStore.getAccessToken(serverId, accountId)
            ?: return fail("Sign in again to continue")
        val authorization = "Bearer $token"
        val api = scopedApiFactory.create(
            baseUrl = baseUrl,
            serverId = serverId,
            accountId = accountId,
            token = token,
            type = DownloaderGatewayApi::class.java,
        )

        return try {
            val jobId = entry.serverJobId ?: createServerJob(entry, api, authorization)
            pollServerJob(entry, api, jobId, baseUrl, authorization)
        } catch (failure: DownloadWorkFailure) {
            if (DownloadFailureCodes.isRetryable(failure.code)) retryOrFail(failure.code)
            else fail(failure.code)
        } catch (_: IOException) {
            retryOrFail(DownloadFailureCodes.Network)
        } catch (_: Exception) {
            fail(DownloadFailureCodes.Unknown)
        }
    }

    private suspend fun createServerJob(
        entry: DownloadEntity,
        api: DownloaderGatewayApi,
        authorization: String,
    ): String {
        val selection = DownloadSelection.fromStorage(entry.quality)
        val response = api.createJob(
            CreateDownloadJobRequest(
                url = entry.videoUrl,
                options = selection.toOptions(),
            ),
            authorization,
        )
        if (!response.isSuccessful) {
            val failureCode = DownloadFailureCodes.fromHttp(response.code(), extractServerError(response))
            throw DownloadWorkFailure(failureCode)
        }
        val created = response.body() ?: throw DownloadWorkFailure(DownloadFailureCodes.InvalidResponse)
        downloadDao.attachServerJob(id.toString(), created.id, created.cached, System.currentTimeMillis())
        return created.id
    }

    private suspend fun pollServerJob(
        entry: DownloadEntity,
        api: DownloaderGatewayApi,
        jobId: String,
        baseUrl: String,
        authorization: String,
    ): Result {
        repeat(POLLS_PER_RUN) {
            if (isStopped) return Result.retry()
            val response = api.job(jobId, authorization)
            if (!response.isSuccessful) {
                val failureCode = DownloadFailureCodes.fromHttp(response.code(), extractServerError(response))
                throw DownloadWorkFailure(failureCode)
            }
            val job = response.body() ?: throw DownloadWorkFailure(DownloadFailureCodes.InvalidResponse)
            when (job.status) {
                DownloadJobStatusDto.Queued -> update("QUEUED", job.progressPercent, job.stage)
                DownloadJobStatusDto.Running -> update("RUNNING", job.progressPercent, job.stage)
                DownloadJobStatusDto.Done -> {
                    val fileName = DownloadFileNames.from(job, entry.title)
                    val systemId = artifactManager.enqueue(baseUrl, jobId, fileName, entry.title)
                    downloadDao.markEnqueued(id.toString(), systemId, fileName, System.currentTimeMillis())
                    return Result.success()
                }
                DownloadJobStatusDto.Failed -> throw DownloadWorkFailure(
                    when (job.errorCode) {
                        DownloadFailureCodes.Cancelled -> DownloadFailureCodes.Cancelled
                        DownloadFailureCodes.InsufficientStorage -> DownloadFailureCodes.InsufficientStorage
                        else -> DownloadFailureCodes.Rejected
                    },
                )
            }
            delay(POLL_DELAY_MILLIS)
        }
        return retryOrFail(DownloadFailureCodes.TimedOut)
    }

    private suspend fun update(
        status: String,
        progress: Int?,
        stage: String?,
        error: String? = null,
    ) {
        downloadDao.updateProgress(
            workId = id.toString(),
            status = status,
            progress = progress,
            stage = stage,
            error = error,
            updatedAt = System.currentTimeMillis(),
        )
        setProgress(workDataOf("status" to status, "progress" to progress, "stage" to stage))
    }

    private suspend fun retryOrFail(message: String): Result =
        if (runAttemptCount < MAX_ATTEMPTS) {
            update("QUEUED", null, null, message)
            Result.retry()
        } else {
            fail(message)
        }

    private suspend fun fail(message: String): Result {
        downloadDao.updateProgress(
            id.toString(),
            "FAILED",
            null,
            null,
            message,
            System.currentTimeMillis(),
        )
        return Result.failure(workDataOf(DownloadWorkContract.ERROR to message))
    }

    private fun DownloadSelection.toOptions(): DownloadJobOptionsDto = when (mode) {
        DownloadMediaMode.Audio -> DownloadJobOptionsDto(
            mode = DownloadModeDto.Audio,
            quality = "best",
            format = "m4a",
            audioCodec = "aac",
        )
        DownloadMediaMode.Video -> DownloadJobOptionsDto(
            mode = DownloadModeDto.Video,
            quality = "${requireNotNull(maxHeight)}p",
            format = "mp4",
            height = maxHeight,
            videoCodec = "h264",
            audioCodec = "aac",
        )
    }

    private class DownloadWorkFailure(val code: String) : Exception(code)

    private companion object {
        const val POLLS_PER_RUN = 120
        const val POLL_DELAY_MILLIS = 2_000L
        const val MAX_ATTEMPTS = 8
    }
}
