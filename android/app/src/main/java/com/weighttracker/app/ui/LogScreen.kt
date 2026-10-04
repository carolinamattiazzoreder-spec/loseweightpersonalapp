package com.weighttracker.app.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weighttracker.app.data.AppState
import com.weighttracker.app.data.WeightEntry
import com.weighttracker.app.data.fmt1
import com.weighttracker.app.ui.theme.WtColors
import java.time.LocalDate
import java.time.ZoneId

/** Accepts both "80.5" and "80,5" (Portuguese keyboards). */
fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

@Composable
fun LogScreen(
    state: AppState,
    onSave: (LocalDate, Double, String) -> Unit,
    onDelete: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    var date by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var weightText by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<WeightEntry?>(null) }

    val weight = parseDecimal(weightText)
    val weightValid = weight != null && weight in 20.0..300.0
    val existing = state.weights.firstOrNull { it.date == date }
    val newestFirst = remember(state.weights) { state.weights.reversed() }
    val placeholder = state.weights.lastOrNull()?.weight?.let(::fmt1) ?: "80.0"

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SectionLabel("Log a weight entry") }
        item {
            OutlinedButton(
                onClick = {
                    val zone = ZoneId.systemDefault()
                    DatePickerDialog(
                        context,
                        { _, y, m, d -> date = LocalDate.of(y, m + 1, d) },
                        date.year, date.monthValue - 1, date.dayOfMonth,
                    ).apply {
                        datePicker.maxDate = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                    }.show()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.DateRange, contentDescription = null)
                Text("  ${date.pretty()}")
            }
        }
        item {
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it },
                label = { Text("Weight (kg)") },
                placeholder = { Text(placeholder) },
                singleLine = true,
                isError = weightText.isNotBlank() && !weightValid,
                supportingText = {
                    if (weightText.isNotBlank() && !weightValid) Text("Enter a weight between 20 and 300 kg")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                placeholder = { Text("e.g. After workout, morning weigh-in…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (existing != null) {
            item {
                Text(
                    "This replaces the existing entry for this date (${fmt1(existing.weight)} kg).",
                    color = WtColors.Orange,
                    fontSize = 12.sp,
                )
            }
        }
        item {
            PrimaryButton(
                text = "💾 Save entry",
                enabled = weightValid,
                onClick = {
                    onSave(date, weight!!, note)
                    note = ""
                    weightText = ""
                    focus.clearFocus()
                },
            )
        }
        if (newestFirst.isNotEmpty()) {
            item { SectionLabel("Entries", Modifier.fillMaxWidth()) }
            items(newestFirst, key = { it.date.toEpochDay() }) { entry ->
                EntryRow(entry) {
                    IconButton(onClick = { pendingDelete = entry }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = WtColors.Muted)
                    }
                }
            }
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete entry?") },
            text = { Text("${fmt1(entry.weight)} kg on ${entry.date.pretty()}") },
            confirmButton = {
                TextButton(onClick = { onDelete(entry.date); pendingDelete = null }) {
                    Text("Delete", color = WtColors.Bad)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}
