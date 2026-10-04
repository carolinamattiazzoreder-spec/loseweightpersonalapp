package com.weighttracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weighttracker.app.ui.theme.WtColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

fun LocalDate.pretty(): String = format(DATE_FORMAT)

val CardShape = RoundedCornerShape(18.dp)

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = WtColors.Muted) {
    Text(
        text = text.uppercase(),
        modifier = modifier.padding(top = 8.dp, bottom = 2.dp),
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
    )
}

/** Rounded card with a gradient/solid background, like the Streamlit stat cards. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    background: Brush = SolidColor(WtColors.SoftBg),
    border: Color? = WtColors.SoftBorder,
    content: @Composable ColumnScope.() -> Unit,
) {
    var m = modifier.clip(CardShape).background(background)
    if (border != null) m = m.border(1.5.dp, border, CardShape)
    Column(m.padding(14.dp), content = content)
}

@Composable
fun StatCard(
    label: String,
    value: String,
    sub: String?,
    modifier: Modifier = Modifier,
    background: Brush = SolidColor(WtColors.SoftBg),
    onDark: Boolean = false,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    Card(modifier, background, border = if (onDark) null else WtColors.SoftBorder) {
        Text(
            label.uppercase(),
            color = if (onDark) Color.White.copy(alpha = 0.65f) else WtColors.Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        )
        Text(
            value,
            modifier = Modifier.padding(top = 4.dp),
            color = if (onDark) Color.White else WtColors.Ink,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 22.sp,
        )
        extra()
        if (sub != null) {
            Text(
                sub,
                modifier = Modifier.padding(top = 6.dp),
                color = if (onDark) Color.White.copy(alpha = 0.55f) else WtColors.Muted,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
fun ProgressBar(percent: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(Color(0xFFF3F0FF))
    ) {
        Box(
            Modifier
                .fillMaxWidth(percent.coerceIn(0, 100) / 100f)
                .height(8.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(WtColors.ProgressGradient)
        )
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = WtColors.Purple),
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}
