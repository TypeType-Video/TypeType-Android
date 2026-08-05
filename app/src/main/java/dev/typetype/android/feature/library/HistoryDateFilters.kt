package dev.typetype.android.feature.library

import dev.typetype.android.domain.library.HistoryDateRange
import dev.typetype.android.domain.library.HistoryQuery
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

enum class HistoryDateSelection {
    All,
    Today,
    ThisWeek,
    ThisMonth,
    SpecificDay,
}

internal fun historyDateRange(
    selection: HistoryDateSelection,
    selectedDateMillis: Long?,
    clock: Clock = Clock.systemDefaultZone(),
): HistoryDateRange? {
    val zone = clock.zone
    val today = LocalDate.now(clock)
    val startDate = when (selection) {
        HistoryDateSelection.All -> return null
        HistoryDateSelection.Today -> today
        HistoryDateSelection.ThisWeek -> today.with(
            TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY),
        )
        HistoryDateSelection.ThisMonth -> today.withDayOfMonth(1)
        HistoryDateSelection.SpecificDay -> selectedDateMillis?.toCalendarDate() ?: return null
    }
    val endMillis = if (selection == HistoryDateSelection.SpecificDay) {
        startDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    } else {
        HISTORY_OPEN_END_MILLIS
    }
    return HistoryDateRange(
        fromMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli(),
        toMillis = endMillis,
    )
}

private fun Long.toCalendarDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

internal fun LocalDate.toPickerMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

internal const val HISTORY_OPEN_END_MILLIS = 9_007_199_254_740_991L

internal fun HistoryQuery.remoteFilterKey(): Triple<String, Long?, Long?> =
    Triple(search.trim(), fromMillis, toMillis)
