package com.weighttracker.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.weighttracker.app.data.HoltPoint
import com.weighttracker.app.data.ProjectionCone
import java.time.LocalDate

class XTicksTest {

    private fun d(s: String) = LocalDate.parse(s)

    @Test
    fun longPeriodsShowMonthNames() {
        val ticks = xTicks(d("2026-08-18"), d("2026-11-16"), 90, d("2026-11-16"))
        assertEquals(listOf("set", "out", "nov"), ticks.map { it.label })
        assertEquals(d("2026-09-01"), ticks.first().date)
    }

    @Test
    fun januaryCarriesTheYear() {
        val ticks = xTicks(d("2026-11-01"), d("2027-02-10"), null, d("2026-12-20"))
        assertEquals(listOf("nov", "dez", "jan 27", "fev"), ticks.map { it.label })
    }

    @Test
    fun manyMonthsAreThinnedToAboutSeven() {
        val ticks = xTicks(d("2025-01-01"), d("2026-12-31"), null, d("2026-12-31"))
        assertTrue(ticks.size in 6..8)
    }

    @Test
    fun thirtyDaysShowWeeklyDatesEndingAtTheLastWeighIn() {
        val ticks = xTicks(d("2026-10-17"), d("2026-11-16"), 30, d("2026-11-16"))
        assertEquals(listOf("19 out", "26 out", "2 nov", "9 nov", "16 nov"), ticks.map { it.label })
        assertTrue(ticks.last().strong)
    }

    @Test
    fun sevenDaysShowEveryDay() {
        val ticks = xTicks(d("2026-11-10"), d("2026-11-16"), 7, d("2026-11-16"))
        assertEquals(7, ticks.size)
        assertEquals("10 nov", ticks.first().label)
    }

    @Test
    fun shortWindowAlsoNamesTheMonthItStartsIn() {
        val ticks = xTicks(d("2026-10-03"), d("2026-11-20"), 90, d("2026-11-20"))
        assertEquals(listOf("out", "nov"), ticks.map { it.label })
    }

    @Test
    fun conePartOfTheAxisIsCapped() {
        // 90 days of history, goal ~200 days away: the future part stops 45 days ahead.
        val cone = ProjectionCone(HoltPoint(d("2026-11-20"), 74.0, -0.1), goal = 55.0, fastSlope = -0.12, slowSlope = -0.05)
        assertEquals(d("2027-01-04"), chartEndDate(d("2026-08-22"), d("2026-11-20"), cone))
        // Without a cone the axis ends at the last weigh-in.
        assertEquals(d("2026-11-20"), chartEndDate(d("2026-08-22"), d("2026-11-20"), null))
        // Short history: at least 3 weeks of future.
        assertEquals(d("2026-12-11"), chartEndDate(d("2026-11-13"), d("2026-11-20"), cone))
    }
}
