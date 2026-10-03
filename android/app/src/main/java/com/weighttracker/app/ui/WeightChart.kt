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
import androidx.compose.ui.graphics.Brush
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
import com.weighttracker.app.data.fmt1
import com.weighttracker.app.data.goalProjection
import com.weighttracker.app.ui.theme.WtColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

private val PAD_LEFT = 44.dp
private val PAD_RIGHT = 12.dp
private val PAD_TOP = 30.dp
private val PAD_BOTTOM = 22.dp

private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM")
private val MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yy")

private class ChartRange(val xMin: Long, val xMax: Long, val yMin: Double, val yMax: Double, val step: Double) {
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

private class ChartFrame(
    width: Float,
    height: Float,
    val left: Float,
    padRight: Float,
    val top: Float,
    padBottom: Float,
    val range: ChartRange,
) {
    val right = width - padRight
    val bottom = height - padBottom

    fun x(date: LocalDate): Float =
        left + (date.toEpochDay() - range.xMin).toFloat() / (range.xMax - range.xMin) * (right - left)

    fun y(weight: Double): Float =
        top + ((range.yMax - weight) / (range.yMax - range.yMin)).toFloat() * (bottom - top)
}

@Composable
fun WeightChart(weights: List<WeightEntry>, goal: Double?, modifier: Modifier = Modifier) {
    val projection = remember(weights, goal) { goalProjection(weights, goal) }
    val range = remember(weights, goal, projection) { ChartRange.of(weights, goal, projection) }
    val textMeasurer = rememberTextMeasurer()
    var selected by remember(weights) { mutableStateOf<Int?>(null) }

    val labelStyle = TextStyle(color = WtColors.Muted, fontSize = 10.sp)
    val goalStyle = TextStyle(color = WtColors.Orange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    val tooltipStyle = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(260.dp)
            .pointerInput(weights, range) {
                detectTapGestures { tap ->
                    val frame = ChartFrame(
                        size.width.toFloat(), size.height.toFloat(),
                        PAD_LEFT.toPx(), PAD_RIGHT.toPx(), PAD_TOP.toPx(), PAD_BOTTOM.toPx(), range,
                    )
                    val nearest = weights.indices.minByOrNull { abs(frame.x(weights[it].date) - tap.x) }
                    selected = if (nearest == selected) null else nearest
                }
            }
    ) {
        val frame = ChartFrame(
            size.width, size.height,
            PAD_LEFT.toPx(), PAD_RIGHT.toPx(), PAD_TOP.toPx(), PAD_BOTTOM.toPx(), range,
        )

        // Horizontal grid + y labels
        var v = range.yMin
        while (v <= range.yMax + 1e-6) {
            val y = frame.y(v)
            drawLine(WtColors.Grid, Offset(frame.left, y), Offset(frame.right, y), strokeWidth = 1.dp.toPx())
            val label = textMeasurer.measure("${v.toInt()} kg", labelStyle)
            drawText(label, topLeft = Offset(frame.left - label.size.width - 6.dp.toPx(), y - label.size.height / 2f))
            v += range.step
        }

        // X labels
        val spanDays = range.xMax - range.xMin
        val xFormat = if (spanDays > 180) MONTH_YEAR else SHORT_DATE
        val ticks = 4
        for (i in 0 until ticks) {
            val date = LocalDate.ofEpochDay(range.xMin + spanDays * i / (ticks - 1))
            val label = textMeasurer.measure(date.format(xFormat), labelStyle)
            val x = (frame.x(date) - label.size.width / 2f)
                .coerceIn(0f, maxOf(0f, size.width - label.size.width))
            drawText(label, topLeft = Offset(x, frame.bottom + 6.dp.toPx()))
        }

        val points = weights.map { Offset(frame.x(it.date), frame.y(it.weight)) }

        // Filled area under the weight line
        val area = Path().apply {
            moveTo(points.first().x, frame.bottom)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, frame.bottom)
            close()
        }
        drawPath(
            area,
            Brush.verticalGradient(
                listOf(WtColors.Purple.copy(alpha = 0.22f), WtColors.Purple.copy(alpha = 0.02f)),
                startY = frame.top,
                endY = frame.bottom,
            ),
        )

        // Goal projection (−1 kg/week)
        if (projection.size >= 2) {
            val path = Path()
            projection.forEachIndexed { i, p ->
                val x = frame.x(p.date)
                val y = frame.y(p.weight)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path,
                WtColors.Projection,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(1f, 6.dp.toPx())),
                ),
            )
        }

        // Goal line
        if (goal != null) {
            val y = frame.y(goal)
            drawLine(
                WtColors.Orange,
                Offset(frame.left, y),
                Offset(frame.right, y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
            )
            val label = textMeasurer.measure("🎯 ${fmt1(goal)} kg", goalStyle)
            drawText(label, topLeft = Offset(frame.right - label.size.width, y + 3.dp.toPx()))
        }

        // Weight line + markers
        val line = Path().apply {
            points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
        }
        drawPath(
            line,
            WtColors.Purple,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        points.forEachIndexed { i, p ->
            val isSelected = i == selected
            val radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx()
            drawCircle(if (isSelected) WtColors.Purple else Color.White, radius, p)
            drawCircle(WtColors.Purple, radius, p, style = Stroke(2.5.dp.toPx()))
        }

        // Tooltip for the tapped point
        selected?.takeIf { it in weights.indices }?.let { i ->
            val p = points[i]
            drawLine(
                WtColors.PurpleLight.copy(alpha = 0.5f),
                Offset(p.x, frame.top),
                Offset(p.x, frame.bottom),
                strokeWidth = 1.dp.toPx(),
            )
            val text = textMeasurer.measure("${fmt1(weights[i].weight)} kg  •  ${weights[i].date.pretty()}", tooltipStyle)
            val padH = 8.dp.toPx()
            val padV = 5.dp.toPx()
            val boxW = text.size.width + padH * 2
            val boxH = text.size.height + padV * 2
            val boxX = (p.x - boxW / 2).coerceIn(0f, maxOf(0f, size.width - boxW))
            drawRoundRect(
                WtColors.Ink,
                topLeft = Offset(boxX, 0f),
                size = Size(boxW, boxH),
                cornerRadius = CornerRadius(6.dp.toPx()),
            )
            drawText(text, topLeft = Offset(boxX + padH, padV))
        }
    }
}
