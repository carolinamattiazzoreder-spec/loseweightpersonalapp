package com.weighttracker.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object WtColors {
    val Purple = Color(0xFF7C3AED)
    val PurpleLight = Color(0xFFA855F7)
    val PurpleSoft = Color(0xFFFAF5FF)
    val PurpleBorder = Color(0xFFEDE9FE)
    val Orange = Color(0xFFF97316)
    val OrangeLight = Color(0xFFFB923C)
    val OrangeSoft = Color(0xFFFFF7ED)
    val Indigo = Color(0xFF1E1B4B)
    val IndigoLight = Color(0xFF312E81)
    val Ink = Color(0xFF1F2937)
    val Body = Color(0xFF374151)
    val Muted = Color(0xFF9CA3AF)
    val Subtle = Color(0xFF6B7280)
    val SoftBg = Color(0xFFF9FAFB)
    val SoftBorder = Color(0xFFF0EDF8)
    val Good = Color(0xFF16A34A)
    val GoodBg = Color(0xFFDCFCE7)
    val Bad = Color(0xFFDC2626)
    val BadBg = Color(0xFFFEE2E2)
    val Projection = Color(0xFF10B981)
    val Grid = Color(0x127C3AED)

    val PurpleGradient = Brush.linearGradient(listOf(Purple, PurpleLight))
    val OrangeGradient = Brush.linearGradient(listOf(Orange, OrangeLight))
    val DarkGradient = Brush.linearGradient(listOf(Indigo, IndigoLight))
    val HeroGradient = Brush.linearGradient(listOf(PurpleSoft, OrangeSoft))
    val ProgressGradient = Brush.horizontalGradient(listOf(Purple, Orange))
}

private val LightColors = lightColorScheme(
    primary = WtColors.Purple,
    onPrimary = Color.White,
    primaryContainer = WtColors.PurpleBorder,
    onPrimaryContainer = WtColors.Purple,
    secondary = WtColors.Orange,
    onSecondary = Color.White,
    secondaryContainer = WtColors.PurpleBorder,
    onSecondaryContainer = WtColors.Purple,
    background = Color.White,
    onBackground = WtColors.Ink,
    surface = Color.White,
    onSurface = WtColors.Ink,
    surfaceVariant = WtColors.SoftBg,
    onSurfaceVariant = WtColors.Subtle,
    surfaceContainer = WtColors.PurpleSoft,
    outline = Color(0xFFE5E7EB),
    error = WtColors.Bad,
)

@Composable
fun WeightTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
