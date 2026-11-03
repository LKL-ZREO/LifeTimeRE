package com.lifetimere.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class DailySettlementTimeTest {
    private val date = LocalDate.of(2026, 11, 3)
    private val zone = ZoneId.of("Asia/Shanghai")

    @Test fun daytimeWaitsUntilLocalTenPm() {
        assertEquals(12 * 60 * 60 * 1000L, DailySettlementTime.delayMillis(date, zone, Instant.parse("2026-11-03T02:00:00Z")))
    }

    @Test fun afterCutoffAndOverdueDaysAreReadyImmediately() {
        assertEquals(0L, DailySettlementTime.delayMillis(date, zone, Instant.parse("2026-11-03T14:00:00Z")))
        assertEquals(0L, DailySettlementTime.delayMillis(date, zone, Instant.parse("2026-11-05T02:00:00Z")))
    }

    @Test fun daylightSavingUsesTheRecordedDaysZone() {
        val newYork = ZoneId.of("America/New_York")
        assertEquals(Instant.parse("2026-11-02T03:00:00Z"), DailySettlementTime.dueAt(LocalDate.of(2026, 11, 1), newYork))
    }

    @Test fun customHalfHourChangesTheSameDaysCutoff() {
        val nineThirtyPm = 21 * 60 + 30
        assertEquals(
            Instant.parse("2026-11-03T13:30:00Z"),
            DailySettlementTime.dueAt(date, zone, nineThirtyPm),
        )
        assertEquals(
            30 * 60 * 1000L,
            DailySettlementTime.delayMillis(date, zone, Instant.parse("2026-11-03T13:00:00Z"), nineThirtyPm),
        )
    }
}
