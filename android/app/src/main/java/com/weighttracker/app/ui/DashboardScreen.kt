package com.weighttracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weighttracker.app.data.AppState
import com.weighttracker.app.data.DashboardStats
import com.weighttracker.app.data.WeightEntry
import com.weighttracker.app.data.bmiCategory
import com.weighttracker.app.data.computeStats
import com.weighttracker.app.data.etaDate
import com.weighttracker.app.data.fmt1
import com.weighttracker.app.data.fmt2
import com.weighttracker.app.ui.theme.WtColors
import kotlin.math.abs

@Composable
fun DashboardScreen(state: AppState, modifier: Modifier = Modifier) {
    val stats = remember(state) { computeStats(state) }
    if (stats == null) {
        EmptyDashboard(modifier)
        return
    }
    val weights = state.weights
    val newestFirst = remember(weights) { weights.reversed() }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { HeroCard(stats) }
        item {
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val icon = when {
                    stats.totalChange > 0 -> "▼"
                    stats.totalChange < 0 -> "▲"
                    else -> "—"
                }
                StatCard(
                    "Total change", "$icon ${fmt1(abs(stats.totalChange))} kg", "since ${stats.startDate.pretty()}",
                    Modifier.weight(1f).fillMaxHeight(), WtColors.PurpleGradient, onDark = true,
                )
                StatCard(
                    "BMI",
                    stats.bmi?.let { "${fmt1(it)} · ${bmiCategory(it).label}" } ?: "Set height first",
                    "${state.heightCm} cm",
                    Modifier.weight(1f).fillMaxHeight(), WtColors.OrangeGradient, onDark = true,
                )
            }
        }
        if (stats.goal != null) {
            item {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        "Goal weight", "🎯 ${fmt1(stats.goal)} kg",
                        stats.remaining?.let { "${if (it >= 0) "+" else ""}${fmt1(it)} kg remaining" },
                        Modifier.weight(1f).fillMaxHeight(), WtColors.DarkGradient, onDark = true,
                    )
                    StatCard(
                        "Est. arrival",
                        stats.weeksToGoal?.let { "📅 ${etaDate(it).pretty()}" } ?: "—",
                        "at −1 kg/week",
                        Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
            item {
                StatCard("Progress", "${stats.progressPercent ?: 0}%", null, Modifier.fillMaxWidth()) {
                    ProgressBar(stats.progressPercent ?: 0, Modifier.padding(top = 8.dp, bottom = 4.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text("${fmt1(stats.start)} kg", color = WtColors.Muted, fontSize = 11.sp)
                        Spacer(Modifier.weight(1f))
                        Text("${fmt1(stats.goal)} kg", color = WtColors.Muted, fontSize = 11.sp)
                    }
                }
            }
        }
        item {
            SectionLabel("Weight history" + (stats.trend?.let { " — $it" } ?: ""))
        }
        item {
            Card(background = androidx.compose.ui.graphics.SolidColor(WtColors.PurpleSoft), border = WtColors.PurpleBorder) {
                if (weights.size >= 2) {
                    WeightChart(weights, stats.goal)
                    ChartLegend(showGoal = stats.goal != null)
                    Text(
                        "Tap the chart to see an entry",
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        color = Color(0xFFC4B5FD),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Text("Add at least 2 entries to see your chart.", color = WtColors.Purple, fontSize = 13.sp)
                }
            }
        }
        item { SectionLabel("All entries") }
        items(newestFirst, key = { it.date.toEpochDay() }) { EntryRow(it) }
    }
}

@Composable
private fun HeroCard(stats: DashboardStats) {
    Card(Modifier.fillMaxWidth(), WtColors.HeroGradient, border = WtColors.PurpleBorder) {
        Text("CURRENT WEIGHT", color = WtColors.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(fmt1(stats.current), color = WtColors.Ink, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold)
            Text(" kg", Modifier.padding(bottom = 8.dp), color = WtColors.Muted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        val (text, fg, bg) = when {
            stats.delta < 0 -> Triple("▼ ${fmt2(abs(stats.delta))} kg", WtColors.Good, WtColors.GoodBg)
            stats.delta > 0 -> Triple("▲ ${fmt2(abs(stats.delta))} kg", WtColors.Bad, WtColors.BadBg)
            else -> Triple("— no change", WtColors.Subtle, Color(0xFFF3F4F6))
        }
        Text(
            text,
            Modifier
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(bg)
                .padding(horizontal = 10.dp, vertical = 3.dp),
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ChartLegend(showGoal: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
    ) {
        LegendItem(WtColors.Purple, "Weight")
        if (showGoal) {
            LegendItem(WtColors.Projection, "Projection (−1 kg/wk)")
            LegendItem(WtColors.Orange, "Goal")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(12.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(" $label", color = WtColors.Subtle, fontSize = 11.sp)
    }
}

@Composable
fun EntryRow(entry: WeightEntry, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entry.date.pretty(), color = WtColors.Body, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (entry.note.isNotBlank()) Text(entry.note, color = WtColors.Muted, fontSize = 12.sp)
            }
            Text("${fmt1(entry.weight)} kg", color = WtColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            trailing()
        }
        HorizontalDivider(color = WtColors.SoftBorder)
    }
}

@Composable
private fun EmptyDashboard(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("📭", fontSize = 48.sp)
        Text("No entries yet", Modifier.padding(top = 8.dp), color = WtColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(
            "Go to Log Weight to start tracking, or import a backup in Settings.",
            Modifier.padding(top = 4.dp),
            color = WtColors.Muted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}
