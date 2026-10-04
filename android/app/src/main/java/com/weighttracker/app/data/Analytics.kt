package com.weighttracker.app.data

import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs

const val PROJECTION_RATE_KG_PER_WEEK = 1.0

fun fmt1(value: Double): String = String.format(Locale.US, "%.1f", value)

fun fmt2(value: Double): String = String.format(Locale.US, "%.2f", value)

fun calcBmi(weight: Double, heightCm: Int): Double? {
    if (heightCm <= 0) return null
    val meters = heightCm / 100.0
    return round1(weight / (meters * meters))
}

enum class BmiCategory(val label: String) {
    Underweight("Underweight"),
    Normal("Normal weight"),
    Overweight("Overweight"),
    Obese("Obese"),
}

fun bmiCategory(bmi: Double): BmiCategory = when {
    bmi < 18.5 -> BmiCategory.Underweight
    bmi < 25 -> BmiCategory.Normal
    bmi < 30 -> BmiCategory.Overweight
    else -> BmiCategory.Obese
}

fun weeksToGoal(current: Double, goal: Double, rate: Double = PROJECTION_RATE_KG_PER_WEEK): Double? {
    val diff = current - goal
    return if (diff > 0) round1(diff / rate) else null
}

fun etaDate(weeks: Double, today: LocalDate = LocalDate.now()): LocalDate =
    today.plusDays(Math.round(weeks * 7))

fun trendLabel(weights: List<WeightEntry>): String? {
    if (weights.size < 2) return null
    val recent = weights.takeLast(7).map { it.weight }
    val delta = recent.last() - recent.first()
    if (abs(delta) < 0.1) return "Stable this week"
    val direction = if (delta < 0) "▼" else "▲"
    return "$direction ${fmt1(abs(delta))} kg this week"
}

data class DashboardStats(
    val current: Double,
    val start: Double,
    val startDate: LocalDate,
    /** Change vs. the previous entry (negative = lost weight). */
    val delta: Double,
    /** Start minus current (positive = lost weight). */
    val totalChange: Double,
    val bmi: Double?,
    val goal: Double?,
    val remaining: Double?,
    val weeksToGoal: Double?,
    val progressPercent: Int?,
    val trend: String?,
)

fun computeStats(state: AppState): DashboardStats? {
    val weights = state.weights
    if (weights.isEmpty()) return null
    val current = weights.last().weight
    val start = weights.first().weight
    val previous = if (weights.size > 1) weights[weights.size - 2].weight else current
    val goal = state.goalWeight
    val progress = goal?.let {
        if (start != it) ((start - current) / (start - it) * 100).toInt().coerceIn(0, 100) else 100
    }
    return DashboardStats(
        current = current,
        start = start,
        startDate = weights.first().date,
        delta = round2(current - previous),
        totalChange = round2(start - current),
        bmi = calcBmi(current, state.heightCm),
        goal = goal,
        remaining = goal?.let { round1(current - it) },
        weeksToGoal = goal?.let { weeksToGoal(current, it) },
        progressPercent = progress,
        trend = trendLabel(weights),
    )
}

data class ChartPoint(val date: LocalDate, val weight: Double)

/** Weekly points from the last entry down to the goal at [rate] kg/week. */
fun goalProjection(
    weights: List<WeightEntry>,
    goal: Double?,
    rate: Double = PROJECTION_RATE_KG_PER_WEEK,
): List<ChartPoint> {
    if (goal == null || weights.isEmpty()) return emptyList()
    val last = weights.last()
    val weeks = weeksToGoal(last.weight, goal, rate) ?: return emptyList()
    val count = weeks.toInt() + 2
    return (0 until count).map { i ->
        ChartPoint(last.date.plusWeeks(i.toLong()), maxOf(last.weight - i * rate, goal))
    }
}
