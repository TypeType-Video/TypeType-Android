package dev.typetype.android.data.imports

import dev.typetype.android.data.network.dto.YoutubeTakeoutImportStatsDto
import dev.typetype.android.data.network.dto.YoutubeTakeoutIssueSummaryDto
import dev.typetype.android.data.network.dto.YoutubeTakeoutJobStatusDto

internal enum class YoutubeTakeoutJobAction {
    Preview,
    Poll,
    Complete,
    Fail,
}

internal data class YoutubeTakeoutTotals(
    val imported: Int,
    val skipped: Int,
    val failed: Int,
)

internal fun YoutubeTakeoutJobStatusDto.nextAction(): YoutubeTakeoutJobAction = when {
    status == "failed" -> YoutubeTakeoutJobAction.Fail
    status == "completed" && phase == "completed" -> YoutubeTakeoutJobAction.Complete
    phase == "importing" -> YoutubeTakeoutJobAction.Poll
    else -> YoutubeTakeoutJobAction.Preview
}

internal fun List<YoutubeTakeoutImportStatsDto>.totals() = YoutubeTakeoutTotals(
    imported = sumOf(YoutubeTakeoutImportStatsDto::imported),
    skipped = sumOf(YoutubeTakeoutImportStatsDto::skipped),
    failed = sumOf(YoutubeTakeoutImportStatsDto::failed),
)

internal fun YoutubeTakeoutIssueSummaryDto.visibleCounts(
    legacyWarnings: Int,
    legacyErrors: Int,
): Pair<Int, Int> = maxOf(warnings, legacyWarnings) to maxOf(errors, legacyErrors)
