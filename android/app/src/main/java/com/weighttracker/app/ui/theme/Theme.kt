package com.weighttracker.app.ui.theme

import android.annotation.SuppressLint
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

/** Palette of the "WeightTracker" canvas design (mint ground, deep-blue ink). */
object WtColors {
    val Bg = Color(0xFFF0FDFA)
    val Surface = Color.White
    val Ink = Color(0xFF082F49)
    val Ink2 = Color(0xFF1E4A60)
    val Muted = Color(0xFF3F6274)
    val Subtle = Color(0xFF5B7C8C)
    val Future = Color(0xFF8AA3AF)
    val Disabled = Color(0xFF9BB8C2)

    val Primary = Color(0xFF0369A1)
    val PrimaryDeep = Color(0xFF082F49)
    val Chart = Color(0xFF0284C7)
    val Dots = Color(0xFF7DD3FC)
    val Projection = Color(0xFF14B8A6)

    val Line = Color(0xFFDDF3EE)
    val Field = Color(0xFFBFE3DA)
    val Cell = Color(0xFFE3F6F1)

    val Mint = Color(0xFFA7F3D0)
    val MintInk = Color(0xFF065F46)
    val MintInk2 = Color(0xFF064E3B)
    val Good = Color(0xFF047857)
    val Success = Color(0xFF10B981)

    val Rose = Color(0xFFFFE4E6)
    val RoseMid = Color(0xFFFDA4AF)
    val RoseStrong = Color(0xFFF43F5E)
    val RoseInk = Color(0xFF9F1239)
    val Error = Color(0xFFE11D48)

    val Sky = Color(0xFFE0F2FE)
    val SkyInk = Color(0xFF075985)
    val SkyInk2 = Color(0xFF0C4A6E)

    val Teal = Color(0xFFCCFBF1)
    val TealInk = Color(0xFF115E59)

    val Amber = Color(0xFFFEF3C7)
    val AmberInk = Color(0xFF92400E)
    val AmberInk2 = Color(0xFF78350F)

    val OnPrimary = Color(0xFFF0FDFA)
}

/** Background/foreground pairs rotated across list badges. */
data class Tone(val bg: Color, val fg: Color)

val BadgeTones = listOf(
    Tone(WtColors.Sky, WtColors.SkyInk),
    Tone(WtColors.Rose, WtColors.RoseInk),
    Tone(WtColors.Teal, WtColors.TealInk),
    Tone(WtColors.Amber, WtColors.AmberInk),
    Tone(WtColors.Mint, WtColors.MintInk),
)

private val LightColors = lightColorScheme(
    primary = WtColors.Primary,
    onPrimary = WtColors.OnPrimary,
    primaryContainer = WtColors.Mint,
    onPrimaryContainer = WtColors.Ink,
    secondary = WtColors.Projection,
    onSecondary = Color.White,
    secondaryContainer = WtColors.Mint,
    onSecondaryContainer = WtColors.Ink,
    background = WtColors.Bg,
    onBackground = WtColors.Ink,
    surface = WtColors.Surface,
    onSurface = WtColors.Ink,
    surfaceVariant = WtColors.Cell,
    onSurfaceVariant = WtColors.Muted,
    surfaceContainer = WtColors.Surface,
    surfaceContainerHigh = WtColors.Surface,
    surfaceContainerHighest = WtColors.Surface,
    outline = WtColors.Field,
    error = WtColors.Error,
)

/**
 * Figtree, downloaded into the build by the `downloadFigtree` Gradle task.
 * Falls back to the system font when the download was not possible.
 */
@SuppressLint("DiscouragedApi")
@OptIn(ExperimentalTextApi::class)
@Composable
private fun rememberFigtree(): FontFamily {
    val context = LocalContext.current
    return remember(context) {
        val id = context.resources.getIdentifier("figtree", "font", context.packageName)
        if (id == 0) {
            FontFamily.Default
        } else {
            FontFamily(
                listOf(400, 500, 600, 700, 800).map { w ->
                    Font(
                        resId = id,
                        weight = FontWeight(w),
                        variationSettings = FontVariation.Settings(FontVariation.weight(w)),
                    )
                }
            )
        }
    }
}

private fun Typography.withFamily(f: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = f),
    displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f),
    headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f),
    headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f),
    titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f),
    bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f),
    bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f),
    labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f),
)

@Composable
fun WeightTrackerTheme(content: @Composable () -> Unit) {
    val figtree = rememberFigtree()
    val typography = remember(figtree) { Typography().withFamily(figtree) }
    MaterialTheme(colorScheme = LightColors, typography = typography, content = content)
}
