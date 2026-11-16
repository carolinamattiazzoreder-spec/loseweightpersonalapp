package com.weighttracker.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun changesKeepTwoDecimals() {
        val state = AppState(weights = listOf(entry("2026-10-01", 77.3), entry("2026-10-09", 76.85)), goalWeight = 72.25)
        val stats = computeStats(state)!!
        assertEquals(0.45, stats.lost, 1e-9)
        assertEquals(4.6, stats.remaining!!, 1e-9)
        assertEquals(-0.45, stats.weeklyChange!!, 1e-9)
    }

    @Test
    fun weeklyChangeNeedsAnEntryAWeekOld() {
        assertNull(weeklyChange(listOf(entry("2026-10-01", 80.0), entry("2026-10-04", 79.0))))
    }

    @Test
    fun milestonesEveryFiveKgDownToGoal() {
        val result = milestones(start = 85.0, goal = 70.0, current = 78.4)
        assertEquals(listOf(80.0, 75.0, 70.0), result.map { it.weight })
        assertEquals(1, result.count { it.reached })
        assertEquals(true, result.last().isGoal)
    }

    @Test
    fun milestonesEndAtAGoalOffTheGrid() {
        assertEquals(listOf(80.0, 75.0, 72.0), milestones(85.0, 72.0, 85.0).map { it.weight })
        assertEquals(listOf(90.0, 85.0, 80.0, 75.0, 70.0, 65.0), milestones(95.0, 65.0, 95.0).map { it.weight })
    }

    @Test
    fun milestonesUseTenKgStepsForHugeGoals() {
        assertEquals(listOf(140.0, 130.0, 120.0, 110.0, 100.0, 90.0, 80.0, 75.0), milestones(150.0, 75.0, 150.0).map { it.weight })
    }

    @Test
    fun periodKeepsRecentEntriesButAtLeastTwo() {
        val weights = listOf(entry("2026-01-01", 90.0), entry("2026-09-01", 82.0), entry("2026-10-01", 80.0))
        assertEquals(2, periodWeights(weights, 30).size)
        assertEquals(listOf(82.0, 80.0), periodWeights(weights, 7).map { it.weight })
        assertEquals(3, periodWeights(weights, null).size)
    }

    @Test
    fun monthSummaryComparesWithPreviousMonth() {
        val weights = listOf(entry("2026-09-26", 79.1), entry("2026-10-01", 79.0), entry("2026-10-03", 78.7), entry("2026-10-04", 78.4))
        val summary = monthSummary(weights, YearMonth.of(2026, 10), LocalDate.parse("2026-10-04"))
        assertEquals(-0.7, summary.change!!, 1e-9)
        assertEquals(-0.61, summary.weeklyAverage!!, 1e-9)
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
    fun normalizeDedupesByDateLastWinsAndKeepsTwoDecimals() {
        val result = normalizeWeights(listOf(entry("2026-01-02", 80.04), entry("2026-01-01", 76.85), entry("2026-01-02", 79.964)))
        assertEquals(listOf(entry("2026-01-01", 76.85), entry("2026-01-02", 79.96)), result)
    }

    private fun steadyLoss(days: Int, every: Int = 1, perDay: Double = 0.1, start: Double = 90.0) =
        (0..days step every).map { WeightEntry(LocalDate.parse("2026-08-01").plusDays(it.toLong()), round2(start - perDay * it)) }

    @Test
    fun holtFollowsASteadyLoss() {
        val last = holtTrend(steadyLoss(60)).last()
        assertEquals(84.0, last.level, 0.01)
        assertEquals(-0.1, last.slope, 0.001)
    }

    @Test
    fun holtHandlesSkippedDays() {
        val last = holtTrend(steadyLoss(90, every = 3)).last()
        assertEquals(81.0, last.level, 0.05)
        assertEquals(-0.1, last.slope, 0.005)
    }

    @Test
    fun holtIsFlatForAFlatWeight() {
        val last = holtTrend((0..20).map { WeightEntry(LocalDate.parse("2026-08-01").plusDays(it.toLong()), 80.0) }).last()
        assertEquals(80.0, last.level, 1e-9)
        assertEquals(0.0, last.slope, 1e-9)
    }

    @Test
    fun coneReachesTheGoalBetweenFastAndSlowPace() {
        val weights = steadyLoss(60)
        val cone = projectionCone(holtTrend(weights), goal = 80.0)!!
        // ~84 kg losing ~0.1 kg/day → about 40 days after the last weigh-in
        val expected = weights.last().date.plusDays(40)
        assertTrue(!cone.fastDate.isAfter(expected.plusDays(2)))
        assertTrue(!cone.slowDate!!.isBefore(expected.minusDays(2)))
        assertTrue(!cone.fastDate.isAfter(cone.slowDate))
        assertEquals(80.0, cone.levelAt(cone.fastSlope, cone.endDate), 1e-9)
    }

    @Test
    fun noConeWithoutGoalAtGoalOrWhenGaining() {
        val trend = holtTrend(steadyLoss(60))
        assertNull(projectionCone(trend, null))
        assertNull(projectionCone(trend, 90.0))
        val gaining = holtTrend(steadyLoss(60, perDay = -0.1))
        assertNull(projectionCone(gaining, 70.0))
    }

    @Test
    fun crossingIsTheFirstDayTheTrendIsAtOrBelow() {
        val trend = holtTrend(steadyLoss(60))
        val crossed = firstCrossing(trend, 85.0)!!
        assertTrue(trend.first { it.date == crossed }.level <= 85.0)
        assertTrue(trend.filter { it.date.isBefore(crossed) }.all { it.level > 85.0 })
        assertNull(firstCrossing(trend, 70.0))
    }
}
