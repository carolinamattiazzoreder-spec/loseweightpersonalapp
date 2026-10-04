package com.weighttracker.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class AnalyticsTest {

    private fun entry(date: String, weight: Double) = WeightEntry(LocalDate.parse(date), weight)

    @Test
    fun bmi() {
        assertEquals(31.8, calcBmi(86.7, 165)!!, 0.0)
        assertNull(calcBmi(80.0, 0))
    }

    @Test
    fun weeksToGoalOnlyWhenAboveGoal() {
        assertEquals(21.7, weeksToGoal(86.7, 65.0)!!, 0.0)
        assertNull(weeksToGoal(64.0, 65.0))
    }

    @Test
    fun stats() {
        val state = AppState(
            weights = listOf(entry("2026-08-12", 85.0), entry("2026-09-26", 79.1), entry("2026-10-03", 78.7), entry("2026-10-04", 78.4)),
            goalWeight = 72.0,
            heightCm = 170,
        )
        val stats = computeStats(state)!!
        assertEquals(78.4, stats.current, 0.0)
        assertEquals(-0.3, stats.delta!!, 1e-9)
        assertEquals(6.6, stats.lost, 1e-9)
        assertEquals(6.4, stats.remaining!!, 1e-9)
        assertEquals(50, stats.progressPercent)
        assertEquals(-0.7, stats.weeklyChange!!, 1e-9)
        assertEquals(27.1, stats.bmi!!, 0.0)
        assertEquals(24.9, stats.bmiAtGoal!!, 0.0)
    }

    @Test
    fun noStatsWithoutEntries() {
        assertNull(computeStats(AppState()))
    }

    @Test
    fun weeklyChangeNeedsAnEntryAWeekOld() {
        assertNull(weeklyChange(listOf(entry("2026-10-01", 80.0), entry("2026-10-04", 79.0))))
    }

    @Test
    fun projectionGoesStraightToGoal() {
        val points = goalProjection(listOf(entry("2026-10-04", 78.4)), 72.0)
        assertEquals(listOf(78.4, 72.0), points.map { it.weight })
        assertEquals(LocalDate.parse("2026-11-18"), points.last().date)
    }

    @Test
    fun milestonesEveryTwoKgDownToGoal() {
        val result = milestones(start = 85.0, goal = 72.0, current = 78.4)
        assertEquals(listOf(84.0, 82.0, 80.0, 78.0, 76.0, 74.0, 72.0), result.map { it.weight })
        assertEquals(3, result.count { it.reached })
        assertEquals(true, result.last().isGoal)
    }

    @Test
    fun milestonesUseBiggerStepsForBigGoals() {
        assertEquals(listOf(90.0, 85.0, 80.0, 75.0, 70.0, 65.0), milestones(95.0, 65.0, 95.0).map { it.weight })
    }

    @Test
    fun monthSummaryComparesWithPreviousMonth() {
        val weights = listOf(entry("2026-09-26", 79.1), entry("2026-10-01", 79.0), entry("2026-10-03", 78.7), entry("2026-10-04", 78.4))
        val summary = monthSummary(weights, YearMonth.of(2026, 10), LocalDate.parse("2026-10-04"))
        assertEquals(-0.7, summary.change!!, 1e-9)
        assertEquals(-0.6, summary.weeklyAverage!!, 1e-9)
        assertEquals(3, summary.count)
        assertEquals(4, summary.elapsedDays)
        assertEquals(setOf(1, 3, 4), summary.daysWeighed)
    }

    @Test
    fun monthsRunFromFirstEntryToToday() {
        val months = monthsWithData(listOf(entry("2026-08-12", 85.0)), LocalDate.parse("2026-10-04"))
        assertEquals(listOf(YearMonth.of(2026, 8), YearMonth.of(2026, 9), YearMonth.of(2026, 10)), months)
    }

    @Test
    fun trendSmoothsTowardsEntries() {
        val trend = trendLine(listOf(entry("2026-10-01", 80.0), entry("2026-10-02", 70.0)))
        assertEquals(80.0, trend[0].weight, 1e-9)
        assertEquals(77.0, trend[1].weight, 1e-9)
    }

    @Test
    fun normalizeDedupesByDateLastWins() {
        val result = normalizeWeights(listOf(entry("2026-01-02", 80.04), entry("2026-01-01", 81.0), entry("2026-01-02", 79.96)))
        assertEquals(listOf(entry("2026-01-01", 81.0), entry("2026-01-02", 80.0)), result)
    }
}
