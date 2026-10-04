package com.weighttracker.app.data

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.floor

const val PROJECTION_RATE_KG_PER_WEEK = 1.0

fun fmt1(value: Double): String = String.format(Locale.US, "%.1f", value)

fun calcBmi(weight: Double, heightCm: Int): Double? {
    if (heightCm <= 0) return null
    val meters = heightCm / 100.0
    return round1(weight / (meters * meters))
}

fun weeksToGoal(current: Double, goal: Double, rate: Double = PROJECTION_RATE_KG_PER_WEEK): Double? {
    val diff = current - goal
    return if (diff > 0) round1(diff / rate) else null
}

fun etaDate(weeks: Double, from: LocalDate = LocalDate.now()): LocalDate =
    from.plusDays(Math.round(weeks * 7))

/** Latest weight minus the newest entry at least 7 days older; null without such an entry. */
fun weeklyChange(weights: List<WeightEntry>): Double? {
    val last = weights.lastOrNull() ?: return null
    val reference = weights.lastOrNull { it.date <= last.date.minusDays(7) } ?: return null
    return round1(last.weight - reference.weight)
}

data class DashboardStats(
    val current: Double,
    val lastDate: LocalDate,
    /** Change vs. the previous entry (negative = lost weight); null with a single entry. */
    val delta: Double?,
    val start: Double,
    val startDate: LocalDate,
    val weeklyChange: Double?,
    /** Start minus current (positive = lost weight). */
    val lost: Double,
    val bmi: Double?,
    val goal: Double?,
    /** Current minus goal (positive = still to lose). */
    val remaining: Double?,
    val weeksToGoal: Double?,
    val progressPercent: Int?,
    val bmiAtGoal: Double?,
)

fun computeStats(state: AppState): DashboardStats? {
    val weights = state.weights
    if (weights.isEmpty()) return null
    val current = weights.last().weight
    val start = weights.first().weight
    val goal = state.goalWeight
    val progress = goal?.let {
        if (start != it) ((start - current) / (start - it) * 100).toInt().coerceIn(0, 100) else 100
    }
    return DashboardStats(
        current = current,
        lastDate = weights.last().date,
        delta = if (weights.size > 1) round2(current - weights[weights.size - 2].weight) else null,
        start = start,
        startDate = weights.first().date,
        weeklyChange = weeklyChange(weights),
        lost = round1(start - current),
        bmi = calcBmi(current, state.heightCm),
        goal = goal,
        remaining = goal?.let { round1(current - it) },
        weeksToGoal = goal?.let { weeksToGoal(current, it) },
        progressPercent = progress,
        bmiAtGoal = goal?.let { calcBmi(it, state.heightCm) },
    )
}

data class ChartPoint(val date: LocalDate, val weight: Double)

/** Straight line from the last entry to the goal at [rate] kg/week. */
fun goalProjection(
    weights: List<WeightEntry>,
    goal: Double?,
    rate: Double = PROJECTION_RATE_KG_PER_WEEK,
): List<ChartPoint> {
    if (goal == null || weights.isEmpty()) return emptyList()
    val last = weights.last()
    val weeks = weeksToGoal(last.weight, goal, rate) ?: return emptyList()
    return listOf(ChartPoint(last.date, last.weight), ChartPoint(etaDate(weeks, last.date), goal))
}

/** Exponential moving average of the entries: the "Tendência" line. */
fun trendLine(weights: List<WeightEntry>, alpha: Double = 0.3): List<ChartPoint> {
    var trend = weights.firstOrNull()?.weight ?: return emptyList()
    return weights.map {
        trend += alpha * (it.weight - trend)
        ChartPoint(it.date, trend)
    }
}

data class Milestone(val weight: Double, val reached: Boolean, val isGoal: Boolean)

/** Round-number checkpoints from the start weight down to the goal (at most ~7). */
fun milestones(start: Double, goal: Double, current: Double): List<Milestone> {
    if (goal >= start) return emptyList()
    val span = start - goal
    val step = listOf(1.0, 2.0, 5.0, 10.0, 20.0).firstOrNull { span / it <= 7 } ?: 50.0
    val values = mutableListOf<Double>()
    var v = floor((start - 1e-9) / step) * step
    while (v > goal + 1e-9) {
        values += v
        v -= step
    }
    values += goal
    return values.map { Milestone(it, current <= it + 1e-9, it == goal) }
}

data class MonthSummary(
    val month: YearMonth,
    /** Last weight in the month minus the last weight before it (or the month's first). */
    val change: Double?,
    val weeklyAverage: Double?,
    val count: Int,
    /** Days of the month up to today (whole month for past months). */
    val elapsedDays: Int,
    val daysWeighed: Set<Int>,
)

fun monthSummary(weights: List<WeightEntry>, month: YearMonth, today: LocalDate): MonthSummary {
    val inMonth = weights.filter { YearMonth.from(it.date) == month }
    val thisMonth = YearMonth.from(today)
    val elapsed = when {
        month == thisMonth -> today.dayOfMonth
        month.isAfter(thisMonth) -> 0
        else -> month.lengthOfMonth()
    }
    val days = inMonth.map { it.date.dayOfMonth }.toSet()
    if (inMonth.isEmpty()) return MonthSummary(month, null, null, 0, elapsed, days)
    val base = weights.lastOrNull { it.date < month.atDay(1) } ?: inMonth.first()
    val last = inMonth.last()
    val change = round1(last.weight - base.weight)
    val spanDays = ChronoUnit.DAYS.between(base.date, last.date)
    val weekly = if (spanDays >= 7) round1(change / (spanDays / 7.0)) else null
    return MonthSummary(month, change, weekly, inMonth.size, elapsed, days)
}

/** Months from the first entry up to today (at least the current month). */
fun monthsWithData(weights: List<WeightEntry>, today: LocalDate): List<YearMonth> {
    val end = YearMonth.from(maxOf(today, weights.lastOrNull()?.date ?: today))
    var m = YearMonth.from(weights.firstOrNull()?.date ?: today)
    val months = mutableListOf<YearMonth>()
    while (!m.isAfter(end)) {
        months += m
        m = m.plusMonths(1)
    }
    return months
}
