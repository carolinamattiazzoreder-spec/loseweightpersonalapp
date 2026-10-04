package com.weighttracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weighttracker.app.R
import com.weighttracker.app.data.AppState
import com.weighttracker.app.data.DashboardStats
import com.weighttracker.app.data.computeStats
import com.weighttracker.app.data.etaDate
import com.weighttracker.app.data.goalProjection
import com.weighttracker.app.data.milestones
import com.weighttracker.app.data.monthSummary
import com.weighttracker.app.data.monthsWithData
import com.weighttracker.app.data.trendLine
import com.weighttracker.app.ui.theme.BadgeTones
import com.weighttracker.app.ui.theme.Tone
import com.weighttracker.app.ui.theme.WtColors
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil

@Composable
fun DashboardScreen(
    state: AppState,
    onOpenLog: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    val stats = remember(state) { computeStats(state) }
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { Greeting(today, onOpenSettings) }
        if (stats == null) {
            item { EmptyCard(onOpenLog) }
            return@LazyColumn
        }
        item { HeroCard(stats, today) }
        item { GoalEtaCard(stats, today, onOpenSettings) }
        if (stats.goal != null && stats.goal < stats.start) {
            item { MilestonesCard(stats.start, stats.goal, stats.current) }
        }
        if (state.weights.size >= 2) {
            item { EvolutionCard(state, today) }
        }
        item { MonthCard(state, today) }
        item { RecentCard(state, stats, onOpenLog) }
    }
}

@Composable
private fun Greeting(today: LocalDate, onOpenSettings: () -> Unit) {
    val name = stringResource(R.string.user_name)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(WtColors.Mint),
            contentAlignment = Alignment.Center,
        ) {
            Text(name.take(1).uppercase(), color = WtColors.MintInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f)) {
            Text(
                if (name.isBlank()) "Olá!" else "Olá, $name",
                fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.2).sp, color = WtColors.Ink,
            )
            Text(PtDate.long(today), fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = WtColors.Muted)
        }
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(44.dp).shadow(2.dp, CircleShape, ambientColor = WtColors.Ink, spotColor = WtColors.Ink),
            colors = IconButtonDefaults.iconButtonColors(containerColor = WtColors.Surface, contentColor = WtColors.Ink2),
        ) {
            Icon(WtIcons.Sliders, contentDescription = "Ajustes", modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun EmptyCard(onOpenLog: () -> Unit) {
    WtCard(spacing = 10.dp) {
        CardTitle("Nenhum registro ainda")
        Text(
            "Registre seu primeiro peso para ver o painel. Para trazer dados antigos, importe um backup em Ajustes.",
            fontSize = 13.sp, color = WtColors.Muted,
        )
        PrimaryPillButton("Registrar primeiro peso", onOpenLog)
    }
}

@Composable
private fun HeroCard(stats: DashboardStats, today: LocalDate) {
    val glowA = Color(0x29F0FDFA)
    val glowB = Color(0x3838BDF8)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(WtColors.Primary, WtColors.PrimaryDeep)))
            .drawBehind {
                val r1 = 95.dp.toPx()
                drawCircle(
                    Brush.radialGradient(listOf(glowA, Color.Transparent), Offset(size.width - 45.dp.toPx(), 25.dp.toPx()), r1),
                    r1, Offset(size.width - 45.dp.toPx(), 25.dp.toPx()),
                )
                val r2 = 85.dp.toPx()
                drawCircle(
                    Brush.radialGradient(listOf(glowB, Color.Transparent), Offset(25.dp.toPx(), size.height - 5.dp.toPx()), r2),
                    r2, Offset(25.dp.toPx(), size.height - 5.dp.toPx()),
                )
            }
            .padding(20.dp)
    ) {
        val onHero = WtColors.OnPrimary
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(
                    Modifier.width(26.dp).height(18.dp).clip(RoundedCornerShape(4.dp)).background(onHero.copy(alpha = 0.28f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.width(14.dp).height(9.dp).clip(RoundedCornerShape(2.dp)).background(onHero.copy(alpha = 0.45f)))
                }
                Text(
                    if (stats.lastDate == today) "PESO DE HOJE" else "ÚLTIMO PESO · ${PtDate.dayMonth(stats.lastDate).uppercase()}",
                    color = onHero.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.1.sp,
                    modifier = Modifier.weight(1f),
                )
                stats.delta?.let {
                    Pill("${kgSigned(it)} kg vs. anterior", onHero.copy(alpha = 0.2f), onHero)
                }
            }
            Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.Bottom) {
                Text(kg(stats.current), color = onHero, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp, lineHeight = 46.sp)
                Text(" kg", color = onHero.copy(alpha = 0.85f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 7.dp))
            }
            stats.weeklyChange?.let { w ->
                val text = when {
                    w <= -0.05 -> "▼ ${kg(-w)} kg nesta semana"
                    w >= 0.05 -> "▲ ${kg(w)} kg nesta semana"
                    else -> "Estável nesta semana"
                }
                Text(text, Modifier.padding(top = 6.dp), color = WtColors.Mint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Text(
                    "Início ${kg(stats.start)} kg · ${PtDate.dayMonth(stats.startDate)}",
                    color = onHero.copy(alpha = 0.85f), fontSize = 12.sp, modifier = Modifier.weight(1f),
                )
                stats.goal?.let { Text("Meta ${kg(it)} kg", color = onHero.copy(alpha = 0.85f), fontSize = 12.sp) }
            }
            if (stats.goal != null && stats.progressPercent != null) {
                Box(
                    Modifier.padding(top = 8.dp).fillMaxWidth().height(9.dp).clip(RoundedCornerShape(999.dp))
                        .background(onHero.copy(alpha = 0.22f))
                ) {
                    Box(
                        Modifier.fillMaxWidth(stats.progressPercent / 100f).fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp)).background(WtColors.Mint)
                    )
                }
                Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    val lostText = if (stats.lost >= 0) "${kg(stats.lost)} kg perdidos" else "${kg(-stats.lost)} kg ganhos"
                    Text("$lostText · ${stats.progressPercent}%", color = onHero, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    val remaining = stats.remaining ?: 0.0
                    Text(
                        if (remaining > 0) "faltam ${kg(remaining)} kg" else "meta atingida!",
                        color = onHero, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalEtaCard(stats: DashboardStats, today: LocalDate, onOpenSettings: () -> Unit) {
    WtCard(spacing = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconCircle(WtIcons.Flag, Tone(WtColors.Rose, WtColors.RoseInk))
            CardTitle("Quando chego na meta", Modifier.weight(1f))
            stats.goal?.let { Pill("${kg(it)} kg", WtColors.Line, WtColors.Ink2) }
        }
        val goal = stats.goal
        val weeks = stats.weeksToGoal
        when {
            goal == null -> {
                Text("Defina um peso meta para ver quando você chega lá.", fontSize = 13.sp, color = WtColors.Muted)
                TextButton(onClick = onOpenSettings, contentPadding = PaddingValues(0.dp)) {
                    Text("Definir meta em Ajustes", color = WtColors.Primary, fontWeight = FontWeight.Bold)
                }
            }
            weeks == null -> {
                Text("Você chegou na meta!", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = WtColors.Good)
                Text("Atualize a meta em Ajustes para continuar acompanhando.", fontSize = 13.sp, color = WtColors.Muted)
            }
            else -> {
                val eta = etaDate(weeks, today)
                val days = Math.round(weeks * 7)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Perdendo 1 kg por semana", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WtColors.Muted)
                    Text(
                        PtDate.eta(eta, today), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.6).sp, lineHeight = 38.sp, color = WtColors.RoseInk,
                    )
                    Text("em $days dias · ${kg(weeks)} semanas", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WtColors.Ink2)
                }
                val bars = ceil(weeks).toInt().coerceIn(1, 14)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(bars) { i ->
                            val color = when {
                                i == 0 -> WtColors.RoseStrong
                                i % 2 == 1 -> WtColors.RoseMid
                                else -> WtColors.Rose
                            }
                            Box(Modifier.weight(1f).height(9.dp).clip(RoundedCornerShape(999.dp)).background(color))
                        }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Text("Hoje · ${kg(stats.current)}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = WtColors.Muted, modifier = Modifier.weight(1f))
                        Text("${PtDate.dayMonth(eta)} · ${kg(goal)}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = WtColors.Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestonesCard(start: Double, goal: Double, current: Double) {
    val items = remember(start, goal, current) { milestones(start, goal, current) }
    if (items.isEmpty()) return
    val reached = items.count { it.reached }
    val nextIndex = items.indexOfFirst { !it.reached }
    WtCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardTitle("Marcos", Modifier.weight(1f))
            Pill("$reached de ${items.size}", WtColors.Mint, WtColors.MintInk)
        }
        val lastReached = items.indexOfLast { it.reached }
        Row(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    val half = 15.dp.toPx()
                    val y = 14.dp.toPx()
                    val stroke = 3.dp.toPx()
                    drawLine(WtColors.Line, Offset(half, y), Offset(size.width - half, y), stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    if (lastReached > 0 && items.size > 1) {
                        val x = half + (size.width - 2 * half) * lastReached / (items.size - 1)
                        drawLine(WtColors.Success, Offset(half, y), Offset(x, y), stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    }
                },
            horizontalArrangement = if (items.size > 1) Arrangement.SpaceBetween else Arrangement.Center,
        ) {
            items.forEachIndexed { i, m ->
                val isNext = i == nextIndex
                Column(Modifier.width(30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    var circle = Modifier.size(28.dp).clip(CircleShape).background(
                        when {
                            m.reached -> WtColors.Success
                            isNext -> WtColors.Surface
                            else -> WtColors.Cell
                        }
                    )
                    if (isNext) circle = circle.border(3.dp, WtColors.Success, CircleShape)
                    Box(circle, contentAlignment = Alignment.Center) {
                        Text(
                            when {
                                m.reached -> "✓"
                                m.isGoal -> "★"
                                else -> ""
                            },
                            color = if (m.reached) Color.White else WtColors.Primary,
                            fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    Text(
                        kgShort(m.weight),
                        color = if (m.reached || isNext) WtColors.Ink else WtColors.Muted,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                    )
                }
            }
        }
        val next = items.getOrNull(nextIndex)
        Text(
            if (next == null) "Todos os marcos concluídos!"
            else "Próximo marco: ${kg(next.weight)} kg — faltam ${kg(current - next.weight)} kg.",
            fontSize = 12.sp, fontWeight = FontWeight.Medium, color = WtColors.Muted,
        )
    }
}

@Composable
private fun EvolutionCard(state: AppState, today: LocalDate) {
    val weights = state.weights
    val goal = state.goalWeight
    val trend = remember(weights) { trendLine(weights) }
    val projection = remember(weights, goal) { goalProjection(weights, goal) }
    val range = remember(weights, goal, projection) { ChartRange.of(weights, goal, projection) }
    WtCard(spacing = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardTitle("Evolução", Modifier.weight(1f))
            Text(
                "${PtDate.dayMonth(weights.first().date)} – ${PtDate.dayMonth(range.endDate)}",
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = WtColors.Muted,
            )
        }
        WeightChart(weights, trend, projection, goal, range, today)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        ) {
            LegendDot(WtColors.Dots, "Pesagens")
            LegendLine(WtColors.Chart, "Tendência")
            if (projection.isNotEmpty()) LegendLine(WtColors.Projection, "1 kg/sem")
            if (goal != null) LegendLine(WtColors.RoseStrong, "Meta")
        }
        Text(
            "Toque no gráfico para ver uma pesagem",
            Modifier.fillMaxWidth(), fontSize = 11.sp, color = WtColors.Subtle, textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = WtColors.Ink2)
    }
}

@Composable
private fun LegendLine(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.width(14.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = WtColors.Ink2)
    }
}

@Composable
private fun MonthCard(state: AppState, today: LocalDate) {
    val months = remember(state.weights, today) { monthsWithData(state.weights, today) }
    var index by rememberSaveable(months.size) { mutableIntStateOf(months.lastIndex) }
    val month = months[index.coerceIn(0, months.lastIndex)]
    val summary = remember(state.weights, month, today) { monthSummary(state.weights, month, today) }
    val thisMonth = YearMonth.from(today)

    WtCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MonthNavButton(WtIcons.ChevronLeft, "Mês anterior", enabled = index > 0) { index-- }
            Text(
                PtDate.monthYear(month), Modifier.weight(1f),
                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = WtColors.Ink, textAlign = TextAlign.Center,
            )
            MonthNavButton(WtIcons.ChevronRight, "Próximo mês", enabled = index < months.lastIndex) { index++ }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            MonthTile("No mês", summary.change?.let { "${kgSigned(it)} kg" } ?: "—", WtColors.Mint, WtColors.MintInk, WtColors.MintInk2, Modifier.weight(1f))
            MonthTile("Média/sem.", summary.weeklyAverage?.let { "${kgSigned(it)} kg" } ?: "—", WtColors.Sky, WtColors.SkyInk, WtColors.SkyInk2, Modifier.weight(1f))
            MonthTile(
                "Pesagens",
                if (month == thisMonth) "${summary.count} de ${summary.elapsedDays}" else summary.count.toString(),
                WtColors.Amber, WtColors.AmberInk, WtColors.AmberInk2, Modifier.weight(1f),
            )
        }
        CalendarGrid(month, summary.daysWeighed, today)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(4.dp)).background(WtColors.Chart))
                Text("Pesou", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = WtColors.Ink2)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).border(2.dp, WtColors.Primary, RoundedCornerShape(4.dp)))
                Text("Hoje", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = WtColors.Ink2)
            }
        }
    }
}

@Composable
private fun MonthNavButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(44.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = WtColors.Line,
            contentColor = WtColors.Ink2,
            disabledContainerColor = WtColors.Line.copy(alpha = 0.5f),
            disabledContentColor = WtColors.Future,
        ),
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun MonthTile(label: String, value: String, bg: Color, labelColor: Color, valueColor: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(bg).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = labelColor, maxLines = 1)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1)
    }
}

@Composable
private fun CalendarGrid(month: YearMonth, weighed: Set<Int>, today: LocalDate) {
    val letters = listOf("D", "S", "T", "Q", "Q", "S", "S")
    val leading = month.atDay(1).dayOfWeek.value % 7 // Sunday first
    val cells: List<Int?> = List(leading) { null } + (1..month.lengthOfMonth()).toList()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            letters.forEach {
                Text(it, Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WtColors.Muted, textAlign = TextAlign.Center)
            }
        }
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 0 until 7) {
                    val day = week.getOrNull(i)
                    if (day == null) {
                        Spacer(Modifier.weight(1f).height(34.dp))
                        continue
                    }
                    val date = month.atDay(day)
                    val did = day in weighed
                    val future = date > today
                    val shape = RoundedCornerShape(12.dp)
                    var cell = Modifier.weight(1f).height(34.dp).clip(shape).background(
                        when {
                            did -> WtColors.Chart
                            future -> Color.Transparent
                            else -> WtColors.Cell
                        }
                    )
                    if (date == today) cell = cell.border(2.dp, WtColors.Primary, shape)
                    Box(cell, contentAlignment = Alignment.Center) {
                        Text(
                            day.toString(),
                            fontSize = 12.sp,
                            fontWeight = if (did) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                did -> Color.White
                                future -> WtColors.Future
                                else -> WtColors.Ink2
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentCard(state: AppState, stats: DashboardStats, onOpenLog: () -> Unit) {
    val weights = state.weights
    val recent = remember(weights) { weights.indices.reversed().take(3) }
    WtCard(padding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 10.dp), spacing = 0.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardTitle("Últimos registros", Modifier.weight(1f))
            TextButton(onClick = onOpenLog) {
                Text("Ver todos", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WtColors.Primary)
            }
        }
        recent.forEachIndexed { n, i ->
            val entry = weights[i]
            val delta = if (i > 0) entry.weight - weights[i - 1].weight else null
            EntryRow(entry, BadgeTones[n % BadgeTones.size], Modifier.padding(vertical = 7.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("${kg(entry.weight)} kg", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WtColors.Ink)
                    if (delta != null) {
                        Text(
                            "${kgSigned(delta)} kg",
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = when {
                                delta <= -0.05 -> WtColors.Good
                                delta >= 0.05 -> WtColors.RoseInk
                                else -> WtColors.Muted
                            },
                        )
                    }
                }
            }
        }
        HorizontalDivider(Modifier.padding(top = 4.dp), color = WtColors.Line)
        Row(Modifier.padding(top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                (stats.bmi?.let { "IMC ${kg(it)} · " } ?: "") + "altura ${state.heightCm} cm",
                Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = WtColors.Muted,
            )
            stats.bmiAtGoal?.let { Pill("Na meta: ${kg(it)}", WtColors.Amber, WtColors.AmberInk) }
        }
    }
}
