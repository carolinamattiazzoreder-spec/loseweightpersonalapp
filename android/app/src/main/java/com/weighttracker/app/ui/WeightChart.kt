package com.weighttracker.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weighttracker.app.data.HoltPoint
import com.weighttracker.app.data.ProjectionCone
import com.weighttracker.app.data.WeightEntry
import com.weighttracker.app.ui.theme.WtColors
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

private val PAD_TOP = 26.dp
private val PAD_BOTTOM = 22.dp
private val Y_LABELS = 26.dp

/** A label under the chart; [strong] marks the last weigh-in. */
data class XTick(val date: LocalDate, val label: String, val strong: Boolean = false)

/**
 * X-axis labels: month names for long periods (90 days, everything), weekly dates for
 * 30 days and daily dates for 7 days, so the month is always readable.
 */
fun xTicks(start: LocalDate, end: LocalDate, periodDays: Long?, lastEntry: LocalDate): List<XTick> {
    if (periodDays != null && periodDays < 90) {
        val every = if (periodDays <= 7) 1L else 7L
        val ticks = mutableListOf<XTick>()
        var d = lastEntry
        while (!d.isBefore(start)) {
            ticks += XTick(d, PtDate.dayMonth(d), strong = d == lastEntry)
            d = d.minusDays(every)
        }
        return ticks.reversed()
    }
    val months = mutableListOf<LocalDate>()
    var m = start.withDayOfMonth(1)
    if (m.isBefore(start)) m = m.plusMonths(1)
    while (!m.isAfter(end)) {
        months += m
        m = m.plusMonths(1)
    }
    if (months.isEmpty()) {
        return listOf(XTick(start, PtDate.dayMonth(start)), XTick(end, PtDate.dayMonth(end)))
    }
    val step = maxOf(1, ceil(months.size / 7.0).toInt())
    return months.filterIndexed { i, _ -> i % step == 0 }.map {
        XTick(it, PtDate.month3(it) + if (it.monthValue == 1) " " + (it.year % 100).toString().padStart(2, '0') else "")
    }
}

/** Last date on the chart: the cone's end when it is shown, else the last weigh-in. */
fun chartEndDate(weights: List<WeightEntry>, cone: ProjectionCone?): LocalDate {
    val last = weights.last().date
    return cone?.endDate?.takeIf { it.isAfter(last) } ?: last
}

private class ChartWindow(
    val xMin: Long,
    val xMax: Long,
    val yMin: Double,
    val yMax: Double,
    val step: Double,
) {
    companion object {
        fun of(weights: List<WeightEntry>, goal: Double?, cone: ProjectionCone?): ChartWindow {
            val values = weights.map { it.weight } + listOfNotNull(goal)
            val lo = values.min()
            val hi = values.max()
            val span = hi - lo
            val step = when {
                span <= 3 -> 1.0
                span <= 8 -> 2.0
                span <= 30 -> 5.0
                span <= 60 -> 10.0
                else -> 20.0
            }
            val xMin = weights.first().date.toEpochDay()
            val xMax = chartEndDate(weights, cone).toEpochDay()
            return ChartWindow(
                xMin = xMin,
                xMax = if (xMax > xMin) xMax else xMin + 1,
                yMin = floor((lo - 0.3) / step) * step,
                yMax = ceil((hi + 0.3) / step) * step,
                step = step,
            )
        }
    }
}

private class ChartFrame(width: Float, height: Float, labels: Float, val top: Float, padBottom: Float, val window: ChartWindow) {
    val left = 0f
    val right = width - labels
    val bottom = height - padBottom

    fun x(date: LocalDate): Float =
        left + (date.toEpochDay() - window.xMin).toFloat() / (window.xMax - window.xMin) * (right - left)

    fun y(weight: Double): Float =
        top + ((window.yMax - weight) / (window.yMax - window.yMin)).toFloat() * (bottom - top)
}

/**
 * The "Evolução" chart: weigh-ins as dots, the Holt trend colored green while going down
 * and rose while going up, 5 kg milestone lines with a ring where the trend crossed them,
 * the goal line and the projection cone. The data draws in from the left when it changes;
 * tap or drag to read a weigh-in.
 */
@Composable
fun WeightChart(
    weights: List<WeightEntry>,
    deltas: List<Double?>,
    trend: List<HoltPoint>,
    goal: Double?,
    cone: ProjectionCone?,
    milestones: List<Pair<Double, LocalDate?>>,
    periodDays: Long?,
    modifier: Modifier = Modifier,
    selectedIndex: Int? = null,
    onSelect: (Int?) -> Unit = {},
) {
    val textMeasurer = rememberTextMeasurer()
    val window = remember(weights, goal, cone) { ChartWindow.of(weights, goal, cone) }
    val ticks = remember(window, periodDays) {
        xTicks(LocalDate.ofEpochDay(window.xMin), LocalDate.ofEpochDay(window.xMax), periodDays, weights.last().date)
    }

    // 0 → 1 while the data draws in from the left. Read only in the draw phase.
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(weights, goal, cone) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }

    /** Index of the weigh-in closest to [x], for a plot area [plotWidth] wide. */
    fun nearest(plotWidth: Float, x: Float): Int? {
        if (plotWidth <= 0f) return null
        val frame = ChartFrame(plotWidth, 1f, 0f, 0f, 0f, window)
        return weights.indices.minByOrNull { abs(frame.x(weights[it].date) - x) }
    }

    val axisStyle = TextStyle(color = WtColors.Muted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    val strongAxisStyle = axisStyle.copy(color = WtColors.Ink, fontWeight = FontWeight.Bold)
    val goalStyle = TextStyle(color = WtColors.Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    val milestoneStyle = TextStyle(color = WtColors.Good, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    val tooltipStyle = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(220.dp)
            .pointerInput(weights, window) {
                detectTapGestures { tap ->
                    val hit = nearest(size.width - Y_LABELS.toPx(), tap.x)
                    onSelect(if (hit == selectedIndex) null else hit)
                }
            }
            .pointerInput(weights, window) {
                // Horizontal only, so the page still scrolls vertically over the chart.
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    onSelect(nearest(size.width - Y_LABELS.toPx(), change.position.x))
                }
            }
    ) {
        val frame = ChartFrame(size.width, size.height, Y_LABELS.toPx(), PAD_TOP.toPx(), PAD_BOTTOM.toPx(), window)
        val progress = reveal.value
        val hairline = 1.dp.toPx()

        // Horizontal grid with values at the right edge
        var v = window.yMin
        while (v <= window.yMax + 1e-6) {
            val y = frame.y(v)
            drawLine(WtColors.Line, Offset(frame.left, y), Offset(size.width, y), strokeWidth = hairline)
            val label = textMeasurer.measure(kgShort(v), axisStyle)
            drawText(label, topLeft = Offset(size.width - label.size.width, y - label.size.height - hairline))
            v += window.step
        }

        // Vertical grid and labels: months, weeks or days
        val labelY = frame.bottom + 6.dp.toPx()
        var lastLabelEnd = Float.NEGATIVE_INFINITY
        ticks.forEach { tick ->
            val x = frame.x(tick.date)
            if (x < frame.left - 1f || x > frame.right + 1f) return@forEach
            drawLine(WtColors.Line, Offset(x, frame.top), Offset(x, frame.bottom), strokeWidth = hairline)
            val label = textMeasurer.measure(tick.label, if (tick.strong) strongAxisStyle else axisStyle)
            val lx = (x - label.size.width / 2f).coerceIn(0f, maxOf(0f, size.width - label.size.width))
            if (lx > lastLabelEnd + 4.dp.toPx()) {
                drawText(label, topLeft = Offset(lx, labelY))
                lastLabelEnd = lx + label.size.width
            }
        }

        // Milestone lines (5 kg)
        val dash = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 4.dp.toPx()))
        milestones.forEach { (weight, _) ->
            if (weight < window.yMin || weight > window.yMax) return@forEach
            val y = frame.y(weight)
            drawLine(WtColors.MilestoneLine, Offset(frame.left, y), Offset(frame.right, y), strokeWidth = hairline, pathEffect = dash)
            val label = textMeasurer.measure(kgShort(weight), milestoneStyle)
            val pad = 3.dp.toPx()
            drawRoundRect(
                WtColors.Surface,
                topLeft = Offset(frame.left, y - label.size.height / 2f),
                size = Size(label.size.width + pad * 2, label.size.height.toFloat()),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
            drawText(label, topLeft = Offset(frame.left + pad, y - label.size.height / 2f))
        }

        // Goal line
        if (goal != null) {
            val y = frame.y(goal)
            drawLine(
                WtColors.RoseStrong,
                Offset(frame.left, y),
                Offset(size.width, y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
            )
            val label = textMeasurer.measure("Meta ${kg(goal)} kg", goalStyle)
            drawText(label, topLeft = Offset(frame.left, y + 3.dp.toPx()))
        }

        val dots = weights.map { Offset(frame.x(it.date), frame.y(it.weight)) }
        val trendPts = trend.map { Offset(frame.x(it.date), frame.y(it.level)) }
        val trendEnd = trendPts.lastOrNull()

        // Projection cone, after the history has drawn in
        val coneAlpha = ((progress - 0.8f) / 0.2f).coerceIn(0f, 1f)
        if (cone != null && trendEnd != null && coneAlpha > 0f) {
            val end = cone.endDate
            val gy = frame.y(cone.goal)
            val area = Path().apply {
                moveTo(trendEnd.x, trendEnd.y)
                lineTo(frame.x(cone.fastDate), gy)
                lineTo(frame.x(end), gy)
                lineTo(frame.x(end), frame.y(cone.levelAt(cone.slowSlope, end)))
                close()
            }
            drawPath(area, WtColors.Projection.copy(alpha = 0.16f * coneAlpha))
            cone.midDate?.let { mid ->
                val midDate = if (mid.isAfter(end)) end else mid
                drawLine(
                    WtColors.Projection.copy(alpha = coneAlpha),
                    trendEnd,
                    Offset(frame.x(midDate), frame.y(cone.levelAt(cone.start.slope, midDate))),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 6.dp.toPx())),
                )
            }
        }

        // History, revealed from the left
        val revealX = frame.left + (trendPts.lastOrNull()?.x ?: frame.right) * progress + 6.dp.toPx()
        clipRect(right = revealX) {
            dots.forEach { drawCircle(WtColors.DotsLight, 2.5.dp.toPx(), it) }

            // Trend: green while going down, rose while going up
            val stroke = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
            for (i in 1 until trendPts.size) {
                val p0 = trendPts[i - 1]
                val p1 = trendPts[i]
                val midX = (p0.x + p1.x) / 2
                val segment = Path().apply {
                    moveTo(p0.x, p0.y)
                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                }
                val color = if (trend[i].level <= trend[i - 1].level) WtColors.Success else WtColors.RoseStrong
                drawPath(segment, color, style = stroke)
            }

            // Rings where the trend crossed a milestone
            val ringStroke = Stroke(2.dp.toPx())
            milestones.forEach { (weight, crossed) ->
                if (crossed == null || weight < window.yMin || weight > window.yMax) return@forEach
                if (crossed.toEpochDay() < window.xMin || crossed.isAfter(weights.last().date)) return@forEach
                val c = Offset(frame.x(crossed), frame.y(weight))
                drawCircle(WtColors.Surface, 4.dp.toPx(), c)
                drawCircle(WtColors.Good, 4.dp.toPx(), c, style = ringStroke)
            }
        }
        if (trendEnd != null && progress >= 1f) {
            drawCircle(Color.White, 5.dp.toPx(), trendEnd)
            drawCircle(WtColors.Ink, 5.dp.toPx(), trendEnd, style = Stroke(3.dp.toPx()))
        }

        // Tooltip for the selected weigh-in
        selectedIndex?.takeIf { it in weights.indices }?.let { i ->
            val p = dots[i]
            drawLine(WtColors.Dots, Offset(p.x, frame.top), Offset(p.x, frame.bottom), strokeWidth = hairline)
            drawCircle(Color.White, 7.dp.toPx(), p)
            drawCircle(WtColors.Chart, 5.dp.toPx(), p)
            val delta = deltas.getOrNull(i)?.let { " · ${kgSigned(it)}" } ?: ""
            val text = textMeasurer.measure("${kg(weights[i].weight)} kg · ${PtDate.dayMonth(weights[i].date)}$delta", tooltipStyle)
            val padH = 8.dp.toPx()
            val padV = 4.dp.toPx()
            val boxW = text.size.width + padH * 2
            val boxH = text.size.height + padV * 2
            val boxX = (p.x - boxW / 2).coerceIn(0f, maxOf(0f, size.width - boxW))
            drawRoundRect(WtColors.Ink, topLeft = Offset(boxX, 0f), size = Size(boxW, boxH), cornerRadius = CornerRadius(8.dp.toPx()))
            drawText(text, topLeft = Offset(boxX + padH, padV))
        }
    }
}
