package com.weighttracker.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weighttracker.app.data.ChartPoint
import com.weighttracker.app.data.WeightEntry
import com.weighttracker.app.ui.theme.WtColors
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

private val PAD_RIGHT = 8.dp
private val PAD_TOP = 22.dp
private val PAD_BOTTOM = 22.dp

/** Data range of the "Evolução" chart: entries, projection and goal. */
class ChartRange private constructor(
    val xMin: Long,
    val xMax: Long,
    val yMin: Double,
    val yMax: Double,
    val step: Double,
) {
    val endDate: LocalDate get() = LocalDate.ofEpochDay(xMax)

    companion object {
        fun of(weights: List<WeightEntry>, goal: Double?, projection: List<ChartPoint>): ChartRange {
            val values = weights.map { it.weight } + projection.map { it.weight } + listOfNotNull(goal)
            val lo = values.min()
            val hi = values.max()
            val span = hi - lo
            val step = when {
                span <= 4 -> 1.0
                span <= 10 -> 2.0
                span <= 30 -> 5.0
                span <= 60 -> 10.0
                else -> 20.0
            }
            val yMin = floor((lo - 1) / step) * step
            val yMax = ceil((hi + 1) / step) * step
            val xMin = weights.first().date.toEpochDay()
            val xMax = maxOf(weights.last().date.toEpochDay(), projection.lastOrNull()?.date?.toEpochDay() ?: xMin)
            return ChartRange(xMin, if (xMax > xMin) xMax else xMin + 1, yMin, yMax, step)
        }
    }
}

private class ChartFrame(width: Float, height: Float, padRight: Float, val top: Float, padBottom: Float, val range: ChartRange) {
    val left = 0f
    val right = width - padRight
    val bottom = height - padBottom

    fun x(date: LocalDate): Float =
        left + (date.toEpochDay() - range.xMin).toFloat() / (range.xMax - range.xMin) * (right - left)

    fun y(weight: Double): Float =
        top + ((range.yMax - weight) / (range.yMax - range.yMin)).toFloat() * (bottom - top)
}

/**
 * Weighings as dots, the smoothed trend line, a dotted projection at −1 kg/week
 * and the dashed goal line. Tap a dot to see its value.
 */
@Composable
fun WeightChart(
    weights: List<WeightEntry>,
    trend: List<ChartPoint>,
    projection: List<ChartPoint>,
    goal: Double?,
    range: ChartRange,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    var selected by remember(weights) { mutableStateOf<Int?>(null) }

    val axisStyle = TextStyle(color = WtColors.Muted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    val goalStyle = TextStyle(color = WtColors.Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    val tooltipStyle = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(210.dp)
            .pointerInput(weights, range) {
                detectTapGestures { tap ->
                    val frame = ChartFrame(
                        size.width.toFloat(), size.height.toFloat(),
                        PAD_RIGHT.toPx(), PAD_TOP.toPx(), PAD_BOTTOM.toPx(), range,
                    )
                    val nearest = weights.indices.minByOrNull { abs(frame.x(weights[it].date) - tap.x) }
                    selected = if (nearest == selected) null else nearest
                }
            }
    ) {
        val frame = ChartFrame(size.width, size.height, PAD_RIGHT.toPx(), PAD_TOP.toPx(), PAD_BOTTOM.toPx(), range)

        // Grid lines with values at the right edge
        var v = range.yMin
        while (v <= range.yMax + 1e-6) {
            val y = frame.y(v)
            drawLine(WtColors.Line, Offset(frame.left, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            val label = textMeasurer.measure(kgShort(v), axisStyle)
            drawText(label, topLeft = Offset(size.width - label.size.width, y - label.size.height - 1.dp.toPx()))
            v += range.step
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

        // Weighings
        val dots = weights.map { Offset(frame.x(it.date), frame.y(it.weight)) }
        dots.forEach { drawCircle(WtColors.Dots, 3.dp.toPx(), it) }

        // Trend (smoothed with mid-point curves)
        val trendPts = trend.map { Offset(frame.x(it.date), frame.y(it.weight)) }
        if (trendPts.size >= 2) {
            val path = Path().apply {
                moveTo(trendPts[0].x, trendPts[0].y)
                for (i in 1 until trendPts.size) {
                    val p0 = trendPts[i - 1]
                    val p1 = trendPts[i]
                    val midX = (p0.x + p1.x) / 2
                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                }
            }
            drawPath(path, WtColors.Chart, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // Projection: from the end of the trend to the goal
        val trendEnd = trendPts.lastOrNull()
        if (trendEnd != null && projection.size >= 2) {
            val end = projection.last()
            val endPt = Offset(frame.x(end.date), frame.y(end.weight))
            drawLine(
                WtColors.Projection, trendEnd, endPt,
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 6.dp.toPx())),
            )
            drawCircle(WtColors.Projection, 5.dp.toPx(), endPt)
        }
        if (trendEnd != null) {
            drawCircle(Color.White, 6.dp.toPx(), trendEnd)
            drawCircle(WtColors.Chart, 6.dp.toPx(), trendEnd, style = Stroke(3.dp.toPx()))
        }

        // X labels: start, last entry ("Hoje" when it is today), projection end
        val labelY = frame.bottom + 6.dp.toPx()
        val first = weights.first().date
        val last = weights.last().date
        val firstLabel = textMeasurer.measure(PtDate.dayMonth(first), axisStyle)
        drawText(firstLabel, topLeft = Offset(0f, labelY))
        val endLabel = if (range.endDate > last) textMeasurer.measure(PtDate.dayMonth(range.endDate), axisStyle) else null
        endLabel?.let { drawText(it, topLeft = Offset(size.width - it.size.width, labelY)) }
        if (last != first) {
            val midLabel = textMeasurer.measure(if (last == today) "Hoje" else PtDate.dayMonth(last), axisStyle)
            val x = (frame.x(last) - midLabel.size.width / 2f)
                .coerceIn(0f, maxOf(0f, size.width - midLabel.size.width))
            val clearsFirst = x > firstLabel.size.width + 8.dp.toPx()
            val clearsEnd = endLabel == null || x + midLabel.size.width < size.width - endLabel.size.width - 8.dp.toPx()
            if (endLabel == null) {
                drawText(midLabel, topLeft = Offset(size.width - midLabel.size.width, labelY))
            } else if (clearsFirst && clearsEnd) {
                drawText(midLabel, topLeft = Offset(x, labelY))
            }
        }

        // Tooltip for the tapped weighing
        selected?.takeIf { it in weights.indices }?.let { i ->
            val p = dots[i]
            drawLine(WtColors.Dots, Offset(p.x, frame.top), Offset(p.x, frame.bottom), strokeWidth = 1.dp.toPx())
            drawCircle(WtColors.Chart, 5.dp.toPx(), p)
            val text = textMeasurer.measure("${kg(weights[i].weight)} kg · ${PtDate.dayMonth(weights[i].date)}", tooltipStyle)
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
