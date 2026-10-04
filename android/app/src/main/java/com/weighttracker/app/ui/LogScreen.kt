package com.weighttracker.app.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weighttracker.app.data.AppState
import com.weighttracker.app.data.WeightEntry
import com.weighttracker.app.ui.theme.BadgeTones
import com.weighttracker.app.ui.theme.Tone
import com.weighttracker.app.ui.theme.WtColors
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun LogScreen(
    state: AppState,
    onSave: (LocalDate, Double, String) -> Unit,
    onDelete: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    var date by rememberSaveable { mutableStateOf(today) }
    var weightText by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<WeightEntry?>(null) }

    val weight = parseDecimal(weightText)
    val weightValid = weight != null && weight in 20.0..300.0
    val existing = state.weights.firstOrNull { it.date == date }
    val newestFirst = remember(state.weights) { state.weights.reversed() }
    val placeholder = state.weights.lastOrNull()?.weight?.let(::kg) ?: "78,4"

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenTitle("Registrar peso") }
        item {
            WtCard(spacing = 13.dp) {
                OutlinedButton(
                    onClick = {
                        val zone = ZoneId.systemDefault()
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> date = LocalDate.of(y, m + 1, d) },
                            date.year, date.monthValue - 1, date.dayOfMonth,
                        ).apply {
                            datePicker.maxDate = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                        }.show()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, WtColors.Field),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WtColors.Ink),
                    contentPadding = PaddingValues(horizontal = 9.dp),
                ) {
                    IconCircle(WtIcons.Calendar, Tone(WtColors.Teal, WtColors.TealInk), size = 30.dp, iconSize = 15.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(PtDate.full(date, today), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                }
                LabeledField(
                    label = "Peso (kg)",
                    value = weightText,
                    onValueChange = { weightText = it },
                    placeholder = placeholder,
                    keyboardType = KeyboardType.Decimal,
                    isError = weightText.isNotBlank() && !weightValid,
                    errorText = "Informe um peso entre 20 e 300 kg",
                    textStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                )
                LabeledField(
                    label = "Nota (opcional)",
                    value = note,
                    onValueChange = { note = it },
                    placeholder = "Ex.: após o treino, pesagem matinal…",
                )
                if (existing != null) {
                    Text(
                        "Substitui o registro de ${PtDate.dayMonth(existing.date)} (${kg(existing.weight)} kg).",
                        fontSize = 12.sp, fontWeight = FontWeight.Medium, color = WtColors.AmberInk,
                    )
                }
                PrimaryPillButton(
                    text = "Salvar registro",
                    enabled = weightValid,
                    onClick = {
                        onSave(date, weight!!, note)
                        note = ""
                        weightText = ""
                        focus.clearFocus()
                    },
                )
            }
        }
        item {
            Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Histórico", Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = WtColors.Ink)
                Pill("${state.weights.size} registros", WtColors.Sky, WtColors.SkyInk)
            }
        }
        if (newestFirst.isNotEmpty()) {
            item {
                WtCard(radius = 28.dp, padding = PaddingValues(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp), spacing = 0.dp) {
                    newestFirst.forEachIndexed { i, entry ->
                        EntryRow(entry, BadgeTones[i % BadgeTones.size], Modifier.padding(vertical = 4.dp)) {
                            Text("${kg(entry.weight)} kg", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WtColors.Ink)
                            IconButton(onClick = { pendingDelete = entry }, modifier = Modifier.size(44.dp)) {
                                Icon(WtIcons.Trash, contentDescription = "Excluir registro", tint = WtColors.Subtle, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { entry ->
        ConfirmDialog(
            title = "Excluir registro?",
            text = "${kg(entry.weight)} kg em ${PtDate.title(entry.date)}",
            action = "Excluir",
            onConfirm = { onDelete(entry.date) },
            onDismiss = { pendingDelete = null },
        )
    }
}
