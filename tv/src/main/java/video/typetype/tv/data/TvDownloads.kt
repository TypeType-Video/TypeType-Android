package video.typetype.tv.data

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import video.typetype.sdk.core.DownloadJob
import video.typetype.sdk.core.DownloadJobStatus
import video.typetype.sdk.core.SessionSnapshot
import video.typetype.sdk.core.TypeTypeError
import video.typetype.sdk.core.TypeTypeResult

public fun TvViewModel.startDownload(option: TvDownloadOption) {
    val current = mutableState.value.downloadJob
    if (current?.status == DownloadJobStatus.Queued || current?.status == DownloadJobStatus.Running ||
        mutableState.value.isSavingDownload
    ) return
    downloadTask?.cancel()
    downloadTask = viewModelScope.launch {
        val session = client.sessions.current()
        mutableState.value = mutableState.value.copy(
            downloadJob = null,
            downloadMessage = null,
            downloadError = null,
            isSavingDownload = false,
        )
        when (val created = client.downloader.create(option.request)) {
            is TypeTypeResult.Failure -> mutableState.value = mutableState.value.copy(
                downloadError = created.error.toUserMessage(),
            )
            is TypeTypeResult.Success -> {
                downloadSession = session
                if (session != null) downloadStateStore.write(session, created.value.id)
                monitorDownload(created.value)
            }
        }
    }
}

internal fun TvViewModel.resumePendingDownload(session: SessionSnapshot) {
    val id = downloadStateStore.read(session) ?: return
    downloadTask?.cancel()
    downloadSession = session
    downloadTask = viewModelScope.launch {
        var failures = 0
        while (isActive) {
            when (val result = client.downloader.job(id)) {
                is TypeTypeResult.Success -> {
                    monitorDownload(result.value)
                    return@launch
                }
                is TypeTypeResult.Failure -> {
                    if (result.error.isMissingDownload()) {
                        downloadStateStore.clear(session)
                        downloadSession = null
                        return@launch
                    }
                    failures++
                    if (!result.error.isRetryableDownloadFailure() || failures >= MAX_DOWNLOAD_RETRIES) {
                        mutableState.value = mutableState.value.copy(downloadError = result.error.toUserMessage())
                        return@launch
                    }
                    mutableState.value = mutableState.value.copy(
                        downloadError = "Connection interrupted. TypeType is trying again.",
                    )
                    delay(downloadRetryDelay(failures))
                }
            }
        }
    }
}

public fun TvViewModel.cancelDownload() {
    val job = mutableState.value.downloadJob ?: return
    if (job.status != DownloadJobStatus.Queued && job.status != DownloadJobStatus.Running) return
    downloadTask?.cancel()
    downloadTask = viewModelScope.launch {
        when (val result = client.downloader.cancel(job.id)) {
            is TypeTypeResult.Success -> mutableState.value = mutableState.value.copy(
                downloadJob = result.value,
                downloadError = null,
            )
            is TypeTypeResult.Failure -> mutableState.value = mutableState.value.copy(
                downloadError = result.error.toUserMessage(),
            )
        }
    }
}

public fun TvViewModel.clearDownload() {
    val job = mutableState.value.downloadJob
    if (mutableState.value.isSavingDownload || job?.status == DownloadJobStatus.Queued ||
        job?.status == DownloadJobStatus.Running
    ) return
    mutableState.value = mutableState.value.copy(
        downloadJob = null,
        downloadMessage = null,
        downloadError = null,
    )
    downloadSession?.let(downloadStateStore::clear)
    downloadSession = null
    if (job != null) viewModelScope.launch { client.downloader.delete(job.id) }
}

public fun TvViewModel.retryDownloadArtifact() {
    val job = mutableState.value.downloadJob?.takeIf { it.status == DownloadJobStatus.Done } ?: return
    if (mutableState.value.isSavingDownload) return
    downloadTask?.cancel()
    downloadTask = viewModelScope.launch { saveArtifact(job) }
}

private suspend fun TvViewModel.monitorDownload(initial: DownloadJob) {
    var job = initial
    var failures = 0
    mutableState.value = mutableState.value.copy(downloadJob = job, downloadError = null)
    while (kotlin.coroutines.coroutineContext.isActive &&
        (job.status == DownloadJobStatus.Queued || job.status == DownloadJobStatus.Running)
    ) {
        delay(DOWNLOAD_POLL_MILLISECONDS)
        when (val result = client.downloader.job(job.id)) {
            is TypeTypeResult.Failure -> {
                failures++
                if (!result.error.isRetryableDownloadFailure() || failures >= MAX_DOWNLOAD_RETRIES) {
                    mutableState.value = mutableState.value.copy(downloadError = result.error.toUserMessage())
                    return
                }
                mutableState.value = mutableState.value.copy(
                    downloadError = "Connection interrupted. TypeType is trying again.",
                )
                delay(downloadRetryDelay(failures))
            }
            is TypeTypeResult.Success -> {
                failures = 0
                job = result.value
                mutableState.value = mutableState.value.copy(downloadJob = job, downloadError = job.error)
            }
        }
    }
    when (job.status) {
        DownloadJobStatus.Done -> saveArtifact(job)
        DownloadJobStatus.Failed -> mutableState.value = mutableState.value.copy(
            downloadError = job.error ?: "The server could not complete this download",
        )
        else -> Unit
    }
}

private suspend fun TvViewModel.saveArtifact(job: DownloadJob) {
    mutableState.value = mutableState.value.copy(isSavingDownload = true, downloadError = null)
    artifactStore.save(client.downloader, job).fold(
        onSuccess = { saved ->
            mutableState.value = mutableState.value.copy(
                isSavingDownload = false,
                downloadMessage = "${saved.fileName} saved in ${saved.location}",
                downloadError = null,
            )
            client.downloader.delete(job.id)
            downloadSession?.let(downloadStateStore::clear)
            downloadSession = null
        },
        onFailure = { failure ->
            mutableState.value = mutableState.value.copy(
                isSavingDownload = false,
                downloadError = failure.message ?: "Android could not save the downloaded file",
            )
        },
    )
}

private fun TypeTypeError.isMissingDownload(): Boolean = this is TypeTypeError.Http && status == 404

private fun TypeTypeError.isRetryableDownloadFailure(): Boolean = when (this) {
    is TypeTypeError.Network -> true
    is TypeTypeError.Http -> status == 408 || status == 425 || status == 429 || status in 500..599
    else -> false
}

private fun downloadRetryDelay(failures: Int): Long =
    (DOWNLOAD_RETRY_BASE_MILLISECONDS * (1L shl (failures - 1))).coerceAtMost(DOWNLOAD_RETRY_MAX_MILLISECONDS)

private const val DOWNLOAD_POLL_MILLISECONDS = 1_500L
private const val DOWNLOAD_RETRY_BASE_MILLISECONDS = 1_000L
private const val DOWNLOAD_RETRY_MAX_MILLISECONDS = 8_000L
private const val MAX_DOWNLOAD_RETRIES = 5
