package com.weighttracker.app.data

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.floor
import kotlin.math.pow

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
    return round2(last.weight - reference.weight)
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
        lost = round2(start - current),
        bmi = calcBmi(current, state.heightCm),
        goal = goal,
        remaining = goal?.let { round2(current - it) },
        weeksToGoal = goal?.let { weeksToGoal(current, it) },
        progressPercent = progress,
        bmiAtGoal = goal?.let { calcBmi(it, state.heightCm) },
    )
}

/** One step of the Holt trend: smoothed weight and its pace (kg per day; negative = losing). */
data class HoltPoint(val date: LocalDate, val level: Double, val slope: Double)

/**
 * Holt's linear (double) exponential smoothing of the entries: a smoothed level plus a
 * smoothed pace, so the trend follows a steady loss without lagging behind it.
 *
 * Time-aware: a gap of n days counts as n daily updates, so after a long pause the level
 * snaps to the new weigh-in, and the pace is extrapolated for at most [paceHorizonDays]
 * across a gap. (Extrapolating a falling pace over months of no weigh-ins pushed the
 * trend far below the real weight, which then showed up as a fake rise.)
 * [alpha] and [beta] are the per-day weights of new weigh-ins and new paces.
 */
fun holtTrend(
    weights: List<WeightEntry>,
    alpha: Double = 0.3,
    beta: Double = 0.1,
    paceHorizonDays: Double = 7.0,
): List<HoltPoint> {
    val first = weights.firstOrNull() ?: return emptyList()
    var level = first.weight
    var slope = 0.0
    val points = mutableListOf(HoltPoint(first.date, level, slope))
    for (i in 1 until weights.size) {
        val days = ChronoUnit.DAYS.between(weights[i - 1].date, weights[i].date).coerceAtLeast(1).toDouble()
        val a = 1 - (1 - alpha).pow(days)
        val b = 1 - (1 - beta).pow(days)
        val predicted = level + slope * minOf(days, paceHorizonDays)
        val newLevel = a * weights[i].weight + (1 - a) * predicted
        slope = b * (newLevel - level) / days + (1 - b) * slope
        level = newLevel
        points += HoltPoint(weights[i].date, level, slope)
    }
    return points
}

/**
 * Where the trend could reach the goal: between the fastest and the slowest pace of the
 * last weeks. [slowDate] is null when the slowest pace is not a loss.
 */
data class ProjectionCone(val start: HoltPoint, val goal: Double, val fastSlope: Double, val slowSlope: Double) {
    private fun dateAt(slope: Double): LocalDate? =
        if (slope < 0) start.date.plusDays(Math.round((start.level - goal) / -slope)) else null

    val fastDate: LocalDate get() = dateAt(fastSlope)!!
    val slowDate: LocalDate? get() = dateAt(slowSlope)
    val midDate: LocalDate? get() = dateAt(start.slope)

    /** Where the cone ends if nothing cuts it short: the slow edge's arrival, at most a year ahead. */
    val endDate: LocalDate
        get() {
            val cap = start.date.plusDays(365)
            val end = slowDate ?: fastDate.plusDays(ChronoUnit.DAYS.between(start.date, fastDate))
            return if (end.isAfter(cap)) cap else end
        }

    /** Trend level at [date] at [slope] kg/day: never below the goal, never above today's level. */
    fun levelAt(slope: Double, date: LocalDate): Double =
        (start.level + slope * ChronoUnit.DAYS.between(start.date, date)).coerceIn(goal, start.level)
}

/** Pace range of the last [windowDays] days; null without a goal, at the goal, or when not losing at all. */
fun projectionCone(trend: List<HoltPoint>, goal: Double?, windowDays: Long = 28): ProjectionCone? {
    val last = trend.lastOrNull() ?: return null
    if (goal == null || last.level <= goal) return null
    val recent = trend.filter { it.date.isAfter(last.date.minusDays(windowDays)) }.map { it.slope }
    val fast = recent.min()
    if (fast >= 0) return null
    return ProjectionCone(last, goal, fast, recent.max())
}

/** First date the trend reached [weight] or less. */
fun firstCrossing(trend: List<HoltPoint>, weight: Double): LocalDate? =
    trend.firstOrNull { it.level <= weight + 1e-9 }?.date

data class Milestone(val weight: Double, val reached: Boolean, val isGoal: Boolean)

/**
 * Checkpoints every 5 kg below the start weight (80, 75, 70…), ending at the goal.
 * Falls back to 10 kg steps when 5 kg would not fit in one row (more than 9).
 */
fun milestones(start: Double, goal: Double, current: Double): List<Milestone> {
    if (goal >= start) return emptyList()
    val step = if ((start - goal) / 5.0 <= 9) 5.0 else 10.0
    val values = mutableListOf<Double>()
    var v = floor((start - 1e-9) / step) * step
    while (v > goal + 1e-9) {
        values += v
        v -= step
    }
    values += goal
    return values.map { Milestone(it, current <= it + 1e-9, it == goal) }
}

/** Entries of the last [days] days (all with null); at least the last 2 entries. */
fun periodWeights(weights: List<WeightEntry>, days: Long?): List<WeightEntry> {
    if (days == null || weights.isEmpty()) return weights
    val cutoff = weights.last().date.minusDays(days)
    val inPeriod = weights.filter { it.date >= cutoff }
    return if (inPeriod.size >= 2) inPeriod else weights.takeLast(2)
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
    val change = round2(last.weight - base.weight)
    val spanDays = ChronoUnit.DAYS.between(base.date, last.date)
    val weekly = if (spanDays >= 7) round2(change / (spanDays / 7.0)) else null
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
