package com.weighttracker.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class AnalyticsTest {

    private fun entry(date: String, weight: Double) = WeightEntry(LocalDate.parse(date), weight)

    @Test
    fun bmi() {
        assertEquals(31.8, calcBmi(86.7, 165)!!, 0.0)
        assertEquals(BmiCategory.Obese, bmiCategory(31.8))
        assertEquals(BmiCategory.Normal, bmiCategory(22.0))
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
            weights = listOf(entry("2025-09-01", 95.0), entry("2026-05-08", 87.3), entry("2026-05-15", 86.7)),
            goalWeight = 65.0,
            heightCm = 165,
        )
        val stats = computeStats(state)!!
        assertEquals(86.7, stats.current, 0.0)
        assertEquals(-0.6, stats.delta, 1e-9)
        assertEquals(8.3, stats.totalChange, 1e-9)
        assertEquals(21.7, stats.remaining!!, 1e-9)
        assertEquals(27, stats.progressPercent)
        assertEquals("▼ 8.3 kg this week", stats.trend)
    }

    @Test
    fun noStatsWithoutEntries() {
        assertNull(computeStats(AppState()))
    }

    @Test
    fun projectionStepsDownWeeklyToGoal() {
        val points = goalProjection(listOf(entry("2026-05-15", 67.5)), 65.0)
        assertEquals(listOf(67.5, 66.5, 65.5, 65.0), points.map { it.weight })
        assertEquals(LocalDate.parse("2026-06-05"), points.last().date)
    }

    @Test
    fun normalizeDedupesByDateLastWins() {
        val result = normalizeWeights(listOf(entry("2026-01-02", 80.04), entry("2026-01-01", 81.0), entry("2026-01-02", 79.96)))
        assertEquals(listOf(entry("2026-01-01", 81.0), entry("2026-01-02", 80.0)), result)
    }
}
