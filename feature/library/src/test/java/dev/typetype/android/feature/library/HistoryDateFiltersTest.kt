package dev.typetype.android.feature.library

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryDateFiltersTest {
    private val zone = ZoneId.of("Europe/Paris")
    private val clock = Clock.fixed(Instant.parse("2026-08-05T12:00:00Z"), zone)

    @Test
    fun allDoesNotRestrictHistory() {
        assertNull(historyDateRange(HistoryDateSelection.All, null, clock))
    }

    @Test
    fun presetsStartAtLocalCalendarBoundaries() {
        val today = requireNotNull(historyDateRange(HistoryDateSelection.Today, null, clock))
        val week = requireNotNull(historyDateRange(HistoryDateSelection.ThisWeek, null, clock))
        val month = requireNotNull(historyDateRange(HistoryDateSelection.ThisMonth, null, clock))

        assertEquals(localStart(2026, 8, 5), today.fromMillis)
        assertEquals(localStart(2026, 8, 3), week.fromMillis)
        assertEquals(localStart(2026, 8, 1), month.fromMillis)
        assertEquals(HISTORY_OPEN_END_MILLIS, today.toMillis)
        assertEquals(HISTORY_OPEN_END_MILLIS, week.toMillis)
        assertEquals(HISTORY_OPEN_END_MILLIS, month.toMillis)
    }

    @Test
    fun specificDayUsesOneLocalCalendarDay() {
        val pickerMillis = LocalDate.of(2026, 7, 14).toPickerMillis()

        val range = requireNotNull(
            historyDateRange(HistoryDateSelection.SpecificDay, pickerMillis, clock),
        )

        assertEquals(localStart(2026, 7, 14), range.fromMillis)
        assertEquals(localStart(2026, 7, 15), range.toMillis)
    }

    @Test
    fun specificDayFollowsDaylightSavingBoundaries() {
        val range = requireNotNull(
            historyDateRange(
                HistoryDateSelection.SpecificDay,
                LocalDate.of(2026, 3, 29).toPickerMillis(),
                clock,
            ),
        )

        assertEquals(localStart(2026, 3, 29), range.fromMillis)
        assertEquals(localStart(2026, 3, 30), range.toMillis)
        assertEquals(23 * 60 * 60 * 1_000L, range.toMillis - range.fromMillis)
    }

    private fun localStart(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(zone).toInstant().toEpochMilli()
}
