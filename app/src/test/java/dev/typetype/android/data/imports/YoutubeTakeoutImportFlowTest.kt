package dev.typetype.android.data.imports

import dev.typetype.android.data.network.dto.YoutubeTakeoutImportStatsDto
import dev.typetype.android.data.network.dto.YoutubeTakeoutIssueSummaryDto
import dev.typetype.android.data.network.dto.YoutubeTakeoutJobStatusDto
import java.io.FileNotFoundException
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubeTakeoutImportFlowTest {
    @Test
    fun completedJobLoadsReport() {
        assertEquals(YoutubeTakeoutJobAction.Complete, status("completed", "completed").nextAction())
    }

    @Test
    fun previewReadyJobCanResumeAtPreview() {
        assertEquals(
            YoutubeTakeoutJobAction.Preview,
            status("completed", "preview_ready").nextAction(),
        )
    }

    @Test
    fun importingAndFailedJobsStayDistinct() {
        assertEquals(YoutubeTakeoutJobAction.Poll, status("running", "importing").nextAction())
        assertEquals(YoutubeTakeoutJobAction.Fail, status("failed", "failed").nextAction())
    }

    @Test
    fun reportBucketsAreAggregated() {
        val totals = listOf(
            YoutubeTakeoutImportStatsDto(imported = 4, skipped = 2, failed = 1),
            YoutubeTakeoutImportStatsDto(imported = 6, skipped = 3, failed = 0),
        ).totals()

        assertEquals(YoutubeTakeoutTotals(imported = 10, skipped = 5, failed = 1), totals)
    }

    @Test
    fun structuredIssueCountsDoNotDoubleCountLegacyLists() {
        val summary = YoutubeTakeoutIssueSummaryDto(total = 5, warnings = 4, errors = 1)

        assertEquals(4 to 1, summary.visibleCounts(legacyWarnings = 4, legacyErrors = 1))
        assertEquals(6 to 2, summary.visibleCounts(legacyWarnings = 6, legacyErrors = 2))
    }

    @Test
    fun uploadNotFoundMeansTheInstanceDoesNotSupportImports() {
        assertEquals(
            YoutubeTakeoutFailureCodes.Unsupported,
            YoutubeTakeoutFailureCodes.fromHttp(404, serverCode = null, uploadRequest = true),
        )
        assertEquals(
            YoutubeTakeoutFailureCodes.JobNotFound,
            YoutubeTakeoutFailureCodes.fromHttp(404, serverCode = null),
        )
    }

    @Test
    fun unavailableDocumentIsNotReportedAsANetworkFailure() {
        val unavailable = IOException(
            "request body failed",
            FileNotFoundException("IMPORT_FILE_UNAVAILABLE"),
        )

        assertEquals(YoutubeTakeoutFailureCodes.Permission, YoutubeTakeoutFailureCodes.fromThrowable(unavailable))
    }

    @Test
    fun oversizedStreamingDocumentIsReportedWithoutRetry() {
        val oversized = IOException("upload failed", IllegalStateException("IMPORT_FILE_TOO_LARGE"))

        assertEquals(YoutubeTakeoutFailureCodes.TooLarge, YoutubeTakeoutFailureCodes.fromThrowable(oversized))
    }

    @Test
    fun transportIoRemainsRetryableNetworkFailure() {
        assertEquals(
            YoutubeTakeoutFailureCodes.Network,
            YoutubeTakeoutFailureCodes.fromThrowable(IOException("connection reset")),
        )
    }

    private fun status(state: String, phase: String) = YoutubeTakeoutJobStatusDto(
        jobId = "job-a",
        status = state,
        phase = phase,
        progress = 50,
        createdAt = 1,
        updatedAt = 2,
        expiresAt = 3,
    )
}
