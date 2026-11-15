package com.weighttracker.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.weighttracker.app.R
import com.weighttracker.app.data.AppState
import com.weighttracker.app.data.WeightEntry
import com.weighttracker.app.ui.theme.Tone
import com.weighttracker.app.ui.theme.WtColors

// ── Motion ───────────────────────────────────────────────────────────────────

/** Timing for the staggered entrance of a screen's cards. */
class Entrance(private val stepMs: Long = 60) {
    private val startedAt = System.currentTimeMillis()

    /** Delay for the [index]-th card; cards composed later (after scrolling) get none. */
    fun delayFor(index: Int): Long =
        (index * stepMs - (System.currentTimeMillis() - startedAt)).coerceAtLeast(0L)
}

/** Fades and slides [content] up when it first appears. */
@Composable
fun Appear(index: Int, entrance: Entrance, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(entrance.delayFor(index))
        progress.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    }
    // Read in the draw phase so the animation never recomposes the content.
    Box(
        modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 20.dp.toPx()
        }
    ) {
        content()
    }
}

/** Counts from 0 to [value], and on to later values; [content] formats the running number. */
@Composable
fun AnimatedNumber(value: Double, durationMillis: Int = 800, content: @Composable (Double) -> Unit) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(value) {
        animated.animateTo(value.toFloat(), tween(durationMillis, easing = FastOutSlowInEasing))
    }
    content(animated.value.toDouble())
}

/** Pill progress bar whose fill grows to [percent]. */
@Composable
fun AnimatedProgressBar(percent: Int, track: Color, fill: Color, modifier: Modifier = Modifier, height: Dp = 9.dp) {
    val fraction = remember { Animatable(0f) }
    LaunchedEffect(percent) {
        fraction.animateTo(percent.coerceIn(0, 100) / 100f, tween(900, easing = FastOutSlowInEasing))
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .background(track)
            .drawBehind {
                val w = size.width * fraction.value
                if (w > 0f) drawRoundRect(fill, size = Size(w, size.height), cornerRadius = CornerRadius(size.height / 2))
            }
    )
}

/** Shrinks the control slightly while pressed. */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource, pressed: Float = 0.96f): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) pressed else 1f,
        spring(stiffness = Spring.StiffnessMediumLow),
        label = "press",
    )
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Segmented pill selector; the highlight moves to the chosen option. */
@Composable
fun <T> PillTabs(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(WtColors.Cell)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            val active = option == selected
            val bg by animateColorAsState(if (active) WtColors.Surface else Color.Transparent, tween(220), label = "tab-bg")
            val fg by animateColorAsState(if (active) WtColors.Ink else WtColors.Muted, tween(220), label = "tab-fg")
            val interaction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(999.dp))
                    .background(bg)
                    .clickable(interaction, indication = null) { onSelect(option) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label(option), color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

/** Name for the greeting: the one saved in Ajustes, else the app default. */
@Composable
fun userName(state: AppState): String = state.name ?: stringResource(R.string.user_name)

/** White rounded card with the design's soft shadow. */
@Composable
fun WtCard(
    modifier: Modifier = Modifier,
    radius: Dp = 32.dp,
    padding: PaddingValues = PaddingValues(18.dp),
    spacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(1.dp, shape, ambientColor = WtColors.Ink, spotColor = WtColors.Ink)
            .clip(shape)
            .background(WtColors.Surface)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp, color = WtColors.Ink)
}

@Composable
fun CardTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = WtColors.Ink)
}

@Composable
fun Pill(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        color = fg,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
}

@Composable
fun IconCircle(icon: ImageVector, tone: Tone, size: Dp = 36.dp, iconSize: Dp = 17.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(tone.bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tone.fg, modifier = Modifier.size(iconSize))
    }
}

/** Round badge with day number and month, e.g. "04 / OUT". */
@Composable
fun DateBadge(entry: WeightEntry, tone: Tone) {
    Column(
        Modifier.size(36.dp).clip(CircleShape).background(tone.bg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            entry.date.dayOfMonth.toString().padStart(2, '0'),
            color = tone.fg, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 13.sp,
        )
        Text(
            PtDate.month3(entry.date).uppercase(),
            color = tone.fg, fontSize = 8.sp, fontWeight = FontWeight.Bold, lineHeight = 9.sp,
        )
    }
}

/** One weight entry: badge, title, note, then whatever [trailing] puts on the right. */
@Composable
fun EntryRow(entry: WeightEntry, tone: Tone, modifier: Modifier = Modifier, trailing: @Composable () -> Unit) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DateBadge(entry, tone)
        Column(Modifier.weight(1f)) {
            Text(PtDate.title(entry.date), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = WtColors.Ink)
            Text(
                entry.note.ifBlank { "Sem nota" },
                fontSize = 11.sp, fontWeight = FontWeight.Medium, color = WtColors.Muted,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        trailing()
    }
}

@Composable
fun PrimaryPillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        modifier = modifier.fillMaxWidth().height(52.dp).pressScale(interaction),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = WtColors.Primary,
            contentColor = WtColors.OnPrimary,
            disabledContainerColor = WtColors.Disabled,
            disabledContentColor = WtColors.OnPrimary,
        ),
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

/** Colored full-width button with a leading icon (Backup card). */
@Composable
fun TonalButton(text: String, icon: ImageVector, tone: Tone, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier.fillMaxWidth().height(52.dp).pressScale(interaction),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = tone.bg, contentColor = tone.fg),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
    }
}

@Composable
fun DangerOutlinedButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.5.dp, WtColors.RoseStrong),
    ) {
        Icon(WtIcons.Trash, contentDescription = null, tint = WtColors.RoseInk, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = WtColors.RoseInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

/** Label above an outlined field, as in the canvas forms. */
@Composable
fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorText: String? = null,
    textStyle: TextStyle = TextStyle(fontSize = 14.sp),
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WtColors.Ink2)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = isError,
            placeholder = placeholder?.let { { Text(it, style = textStyle, color = WtColors.Subtle) } },
            textStyle = textStyle.copy(color = WtColors.Ink),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = WtColors.Primary,
                unfocusedBorderColor = WtColors.Field,
                errorBorderColor = WtColors.Error,
                focusedContainerColor = WtColors.Surface,
                unfocusedContainerColor = WtColors.Surface,
                errorContainerColor = WtColors.Surface,
                cursorColor = WtColors.Primary,
            ),
        )
        if (isError && errorText != null) {
            Text(errorText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = WtColors.Primary)
        }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, action: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = WtColors.Surface,
        title = { Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = WtColors.Ink) },
        text = { Text(text, fontSize = 14.sp, color = WtColors.Ink2) },
        confirmButton = {
            Button(
                onClick = { onConfirm(); onDismiss() },
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WtColors.Rose, contentColor = WtColors.RoseInk),
            ) { Text(action, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = WtColors.Ink2, fontWeight = FontWeight.Bold) }
        },
    )
}
