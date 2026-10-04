package com.weighttracker.app.ui

import androidx.compose.foundation.BorderStroke
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
import com.weighttracker.app.data.WeightEntry
import com.weighttracker.app.ui.theme.Tone
import com.weighttracker.app.ui.theme.WtColors

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
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
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
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
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
